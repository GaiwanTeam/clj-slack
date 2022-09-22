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
            [co.gaiwan.slack.normalize.messages :as normalize-messages]))

(def ?WorkspaceStore
  [:map
   [:workspace-store/users
    [:map-of string? ?User]]
   [:workspace-store/emoji
    [:map-of string? string?]]
   [:workspace-store/channels
    [:map-of string? ?Channel]]])

(defn new-store []
  {:workspace-store/users {}
   :workspace-store/emoji {}
   :workspace-store/channels {}})

(defn channel-message-tree [store channel-id]
  (get-in store [:workspace-store/channels channel-id :channel/message-tree]))

(defn index-by [k coll]
  (into {} (map (juxt k identity)) coll))

(defn add-users [store users]
  (update store :workspace-store/users into (index-by :user/id users)))

(defn add-channels [store channels]
  (update store :workspace-store/channels into (index-by :channel/id channels)))

(defn add-emoji [store emoji]
  (update store :workspace-store/emoji into emoji))

(defn add-channel-event [store channel-id event]
  (update-in store [:workspace-store/channels channel-id :channel/message-tree]
             normalize-messages/add-event event))

(defn add-channel-events [store channel-id events]
  ;; When events come from the history API they are missing the channel value
  (reduce #(add-channel-event %1
                              channel-id
                              (assoc %2 "channel" channel-id))
          store events))

(defn add-raw-event [store event]
  (if-let [channel-id (raw-event/channel-id event)]
    (add-channel-event store channel-id event)
    ;; TODO: handle user and channel add/change/delete events
    store)
  ;; TODO: return store and affected keys
  )

(defn add-channel-messages [store channel-id messages]
  (update-in store [:workspace-store/channels
                    channel-id
                    :channel/message-tree]
             (fnil into (sorted-map))
             (index-by :message/timestamp messages)))
