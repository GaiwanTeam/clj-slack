(ns co.gaiwan.slack.normalize.messages
  "Code for converting messages and related events into our own message-tree format.

  For messages we get data both from the RTM API and from the Web
  API (backfills). While these have some differences they are similar enough
  that we are generally able to produce code that can transparently handle
  either."
  (:require [co.gaiwan.slack.raw-event :as raw-event]))

(defmulti add-event
  "Process a single event, adding it to `message-tree`, which is a (sorted) map, keyed
  by timestamp."
  (fn [message-tree event] (get event "type")))

(defmulti affected-keys
  "Which keys in the message tree are affected by the event? returns a sequence of
  0 (event is ignored), 1 (regular message, reaction), or 2 (reply) timestamps."
  (fn [event] (get event "type")))

(defmethod add-event :default [message-tree _] message-tree)
(defmethod affected-keys :default [event] nil)

(defn add-message
  "Add a message to the message-tree map bassed on its `:message/timestamp`"
  [message-tree message]
  (assert (:message/timestamp message))
  (assoc message-tree (:message/timestamp message) message))

(defn add-thread-reply
  "Add a reply to a message, adding it to a sorted map under `:message/replies`.
  `thread_ts` is the timestamp of the message that is being replied to."
  [message-tree thread_ts message]
  (if (contains? message-tree thread_ts)
    (-> message-tree
        (update-in [thread_ts :message/reply-timestamps]
                   (fnil conj (sorted-set))
                   (:message/timestamp message))
        (assoc (:message/timestamp message) (assoc message :message/thread-ts thread_ts)))
    message-tree))

(defmethod add-event "message"
  [message-tree {:strs [subtype text channel message] :as event}]
  (let [message-ts (raw-event/message-ts event)
        user (raw-event/user-id event)]
    (cond
      (raw-event/regular-message? event)
      (let [message {:message/timestamp message-ts
                     :message/text text
                     :message/channel-id channel
                     :message/user-id user}]
        (if (raw-event/reply? event)
          (let [broadcast? (raw-event/thread-broadcast? event)
                message (cond-> message
                          broadcast?
                          (assoc :message/thread-broadcast? true))]
            (add-thread-reply message-tree (raw-event/parent-ts event) message))
          (add-message message-tree message)))

      (= "message_changed" subtype)
      (if (contains? message-tree message-ts)
        (assoc-in message-tree [message-ts :message/text] (get message "text"))
        message-tree)

      (#{"group_join" "group_leave" "bot_message"} subtype)
      message-tree

      (#{"message_deleted" "tombstone"} subtype)
      (if (contains? message-tree message-ts)
        (update message-tree message-ts
                (fn [m]
                  ;; Originally we just marked messages as "deleted" and
                  ;; left it at that, but since we will propagate this
                  ;; data to the frontend we do actually want to make
                  ;; sure the message text is no longer accessible, so we
                  ;; do a similar thing to what slack does and turn this
                  ;; into a "tombstone"
                  {:message/timestamp message-ts
                   :message/channel-id (:message/channel-id m)
                   :message/deleted? true
                   :message/text "This message was deleted."
                   :message/user-id "USLACKBOT"
                   :message/system? true}))
        message-tree)

      (raw-event/system-message? event)
      (add-message message-tree {:message/timestamp message-ts
                                 :message/text text
                                 :message/channel-id channel
                                 :message/user-id user
                                 :message/system? true})

      (#{"me_message" "reminder_add"} subtype)
      (add-message message-tree {:message/timestamp message-ts
                                 :message/text text
                                 :message/channel-id channel
                                 :message/user-id user
                                 :message/emphasis? true})

      (= "slack_image" subtype)
      (add-message message-tree {:message/timestamp message-ts
                                 :message/text text
                                 :message/channel-id channel
                                 :message/user-id user
                                 :message/image? true})

      :else
      message-tree)))

(defmethod affected-keys "message" [{:strs [ts subtype] :as event}]
  (cond
    (= "message_changed" subtype)
    [ts]
    (#{"channel_archive" "channel_join" "channel_name" "channel_purpose" "channel_topic"} subtype)
    nil
    ;;TODO
    ))

(defmethod add-event "reaction_added" [message-tree {:strs [ts reaction item]}]
  (if (contains? message-tree (get item "ts"))
    (update-in message-tree [(get item "ts") :message/reactions reaction] (fnil inc 0))
    message-tree))

(defmethod affected-keys "reaction_added" [{:strs [ts subtype] :as event}]
  [ts])

(defmethod add-event "reaction_removed" [message-tree {:strs [ts reaction item]}]
  (if (contains? message-tree (get item "ts"))
    (update-in message-tree [(get item "ts") :message/reactions reaction] (fnil dec 0))
    message-tree))

;; Might still want to handle these later
(defmethod add-event "member_join_channel" [message-tree {:strs [ts user]}]
  message-tree)

(defmethod add-event "member_left_channel" [message-tree {:strs [ts user]}]
  message-tree)
