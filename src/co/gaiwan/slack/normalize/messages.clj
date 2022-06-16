(ns co.gaiwan.slack.normalize.messages
  "Code for converting messages and related events into our own data format.

  For messages we get data both from the RTM API and from the Web
  API (backfills). While these have some differences they are similar enough
  that we are generally able to produce code that can transparently handle
  either.")

(defmulti add-event
  "Process a single event, adding it to `events`, which is a (sorted) map, keyed
  by timestamp."
  (fn [events event] (get event "type")))

(defmethod add-event :default [events _] events)

(defn add-message
  "Add a message to the events map bassed on its `:message/timestamp`"
  [events message]
  (assert (:message/timestamp message))
  (assoc events (:message/timestamp message) message))

(defn add-thread-reply
  "Add a reply to a message, adding it to a sorted map under `:message/replies`.
  `thread_ts` is the timestamp of the message that is being replied to."
  [events thread_ts message]
  (if (contains? events thread_ts)
    (update-in events [thread_ts :message/replies]
               (fnil assoc (sorted-map))
               (:message/timestamp message)
               message)
    events))

(defmethod add-event "message"
  [events {:strs [ts subtype text channel user thread_ts message] :as event}]
  (cond
    (or (nil? subtype) (= "bot_message" subtype))
    (let [message {:message/timestamp ts
                   :message/text text
                   :message/channel-id channel
                   :message/user-id user}]
      (if thread_ts
        (add-thread-reply events thread_ts message)
        (add-message events message)))

    (= "message_changed" subtype)
    (assoc-in events [(get message "ts") :message/text] (get message "text"))

    (= "message_replied" subtype)
    events

    (= "thread_broadcast" subtype)
    (let [message {:message/timestamp ts
                   :message/text text
                   :message/channel-id channel
                   :message/user-id user}]
      (-> events
          (add-message (assoc message :message/thread-broadcast? true))
          (add-thread-reply thread_ts message)))

    (#{"group_join" "group_leave" "bot_message"} subtype)
    events

    (#{"message_deleted" "tombstone"} subtype)
    (assoc-in events [ts :message/deleted?] true)

    (#{"channel_archive" "channel_join" "channel_name" "channel_purpose" "channel_topic"} subtype)
    (add-message events {:message/timestamp ts
                         :message/text text
                         :message/channel-id channel
                         :message/user-id user
                         :message/system? true})

    (#{"me_message" "reminder_add"} subtype)
    (add-message events {:message/timestamp ts
                         :message/text text
                         :message/channel-id channel
                         :message/user-id user
                         :message/emphasis? true})

    (= "slack_image" subtype)
    (add-message events {:message/timestamp ts
                         :message/text text
                         :message/channel-id channel
                         :message/user-id user
                         :message/image? true})

    :else
    events))

(defmethod add-event "reaction_added" [events {:strs [ts reaction item]}]
  (update-in events [(get item "ts") :message/reactions reaction] (fnil inc 0)))

(defmethod add-event "reaction_removed" [events {:strs [ts reaction item]}]
  (update-in events [(get item "ts") :message/reactions reaction] (fnil dec 0)))

;; Might still want to handle these later
(defmethod add-event "member_join_channel" [events {:strs [ts user]}]
  events)

(defmethod add-event "member_left_channel" [events {:strs [ts user]}]
  events)



(comment
  (require '[co.gaiwan.slack.archive.partition :as arch])

  (def raw  (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C015DQFEGMT" "2021-10-05"))

  (dotimes [i 10]
    (time
     (count
      (message-seq
       (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C015DQFEGMT" "2021-10-05")))))

  (time
   (count
    (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C015DQFEGMT" "2021-10-05")
    ))


  (message-seq
   (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C014LA21AS3" "2020-07-02"))

  )
