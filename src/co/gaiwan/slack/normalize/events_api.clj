(ns co.gaiwan.slack.normalize.events-api
  "Code for converting events as we get them from the Slack RTM API into our own
  data format.")

(defmulti add-event (fn [events event] (get event "type")))

(defmethod add-event :default [events _] events)

(defn add-message [events message]
  (assoc events (:message/timestamp message) message))

(defn add-thread-reply [events thread_ts message]
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
    (add-message events {:message/timestamp ts
                         :message/text text
                         :message/channel-id channel
                         :message/user-id user})

    (= "message_changed" subtype)
    (assoc-in events [(get message "ts") :message/text] (get message "text"))

    (= "message_replied" subtype)
    (add-thread-reply
     events
     thread_ts
     {:message/timestamp ts
      :message/text text
      :message/channel-id channel
      :message/user-id user})

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

(defn message-data
  "Given a seqable of raw events, normalize them to proper EDN event
  representations. Thread replies and reactions are added to the message they
  refer to, so the result potentially has a single level of nesting."
  [raw-events]
  (map
   (fn [e]
     (if (:message/replies e)
       (update e :message/replies vals)
       e))
   (vals
    (reduce
     add-event
     (sorted-map)
     (sort-by #(get % "ts") raw-events)))))


(comment
  (require '[co.gaiwan.slack.archive.partition :as arch])

  (def raw  (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C015DQFEGMT" "2021-10-05"))

  (dotimes [i 10]
    (time
     (count
      (message-data
       (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C015DQFEGMT" "2021-10-05")))))

  (time
   (count
    (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C015DQFEGMT" "2021-10-05")
    ))


  (message-data
   (arch/slurp-chan-day {:dir "/tmp/gene-archive"} "C014LA21AS3" "2020-07-02"))

  )
