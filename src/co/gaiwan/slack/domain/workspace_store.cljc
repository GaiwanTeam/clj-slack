(ns co.gaiwan.slack.domain.workspace-store
  "Schema and logic for manipulating a workspace-store

  A workspace-store contains data about a complete workspace
  - users
  - custom emoji
  - channels, optionally including message history
  "
  (:require [co.gaiwan.slack.domain.user :refer [?User]]
            [co.gaiwan.slack.domain.channel :refer [?Channel]]
            [co.gaiwan.slack.raw-event :as raw-event]
            [co.gaiwan.slack.normalize.messages :as normalize-messages]
            [co.gaiwan.slack.enrich :as enrich]))

(def ?WorkspaceStore
  [:map
   [:workspace-store/workspace string?]
   [:workspace-store/markdown-handlers map?]
   [:workspace-store/text-handlers {:optional true} map?]
   [:workspace-store/users [:map-of string? ?User]]
   [:workspace-store/emoji [:map-of string? string?]]
   [:workspace-store/channels [:map-of string? ?Channel]]])

(defn new-store [{:keys [workspace markdown-handlers text-handlers]}]
  {:workspace-store/users {}
   :workspace-store/emoji {}
   :workspace-store/channels {}
   :workspace-store/workspace workspace
   :workspace-store/markdown-handlers markdown-handlers
   :workspace-store/text-handlers text-handlers})

(defn channel-message-tree [store channel-id]
  (get-in store [:workspace-store/channels channel-id :channel/message-tree]))

(defn index-by [k coll]
  (into {} (map (juxt k identity)) coll))

(defn add-users [store users]
  (update store :workspace-store/users (fnil into {}) (index-by :user/id users)))

(defn add-channels [store channels]
  (update store :workspace-store/channels (fnil into {}) (index-by :channel/id channels)))

(defn add-emoji [store emoji]
  (update store :workspace-store/emoji (fnil into {}) emoji))

(defn init-channel [store channel-id]
  (assoc-in store [:workspace-store/channels channel-id :channel/message-tree] {}))

(defn add-channel-event [store channel-id event]
  (if (channel-message-tree store channel-id)
    (update-in store [:workspace-store/channels channel-id :channel/message-tree]
               normalize-messages/add-event event)
    store))

(defn add-channel-events [store channel-id events]
  ;; When events come from the history API they are missing the channel value
  (reduce #(add-channel-event %1
                              channel-id
                              (assoc %2 "channel" channel-id))
          store events))

(defn add-raw-event
  "Add a raw event to the store, updating the relevant message in the right
  channel."
  [store event]
  (if-let [channel-id (raw-event/channel-id event)]
    (add-channel-event store channel-id event)
    ;; TODO: handle user and channel add/change/delete events
    store))

(defn add-channel-messages [store channel-id messages]
  (update-in store [:workspace-store/channels
                    channel-id
                    :channel/message-tree]
             (fnil into (sorted-map))
             (index-by :message/timestamp messages)))

(defn update-channels [store f & args]
  (update store
          :workspace-store/channels
          update-vals
          (fn [channel]
            (apply f channel args))))

(defn enrich-all [store]
  (update-channels
   store
   (fn [channel]
     (update
      channel :channel/message-tree
      (fn [message-tree]
        (enrich/enrich message-tree
                       {:users         (:workspace-store/users store)
                        :handlers      (:workspace-store/markdown-handlers store)
                        :text-handlers (:workspace-store/text-handlers store)
                        :org-name      (:workspace-store/workspace store)}))))))

(defn enrich-entries [store message-ids]
  (update-channels
   store
   (fn [channel]
     (update
      channel :channel/message-tree
      (fn [message-tree]
        (enrich/enrich-entries
         message-tree
         message-ids
         {:users         (:workspace-store/users store)
          :handlers      (:workspace-store/markdown-handlers store)
          :text-handlers (:workspace-store/text-handlers store)
          :org-name      (:workspace-store/workspace store)}))))))
