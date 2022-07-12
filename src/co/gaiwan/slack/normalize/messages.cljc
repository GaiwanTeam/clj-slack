(ns co.gaiwan.slack.normalize.messages
  "Code for converting messages and related events into our own message-tree format.

  For messages we get data both from the RTM API and from the Web
  API (backfills). While these have some differences they are similar enough
  that we are generally able to produce code that can transparently handle
  either.")

(defmulti add-event
  "Process a single event, adding it to `message-tree`, which is a (sorted) map, keyed
  by timestamp."
  (fn [message-tree event] (get event "type")))

(defmethod add-event :default [message-tree _] message-tree)

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
    (update-in message-tree [thread_ts :message/replies]
               (fnil assoc (sorted-map))
               (:message/timestamp message)
               message)
    message-tree))

(defmethod add-event "message"
  [message-tree {:strs [ts subtype text channel user thread_ts message] :as event}]
  (cond
    (or (nil? subtype) (= "bot_message" subtype))
    (let [message {:message/timestamp ts
                   :message/text text
                   :message/channel-id channel
                   :message/user-id user}]
      (if thread_ts
        (add-thread-reply message-tree thread_ts message)
        (add-message message-tree message)))

    (= "message_changed" subtype)
    (if (contains? message-tree (get message "ts"))
      (assoc-in message-tree [(get message "ts") :message/text] (get message "text"))
      message-tree)

    (= "message_replied" subtype)
    message-tree

    (= "thread_broadcast" subtype)
    (let [message {:message/timestamp ts
                   :message/text text
                   :message/channel-id channel
                   :message/user-id user}]
      (-> message-tree
          (add-message (assoc message :message/thread-broadcast? true))
          (add-thread-reply thread_ts message)))

    (#{"group_join" "group_leave" "bot_message"} subtype)
    message-tree

    (#{"message_deleted" "tombstone"} subtype)
    (if (contains? message-tree ts)
      (assoc-in message-tree [ts :message/deleted?] true)
      message-tree)

    (#{"channel_archive" "channel_join" "channel_name" "channel_purpose" "channel_topic"} subtype)
    (add-message message-tree {:message/timestamp ts
                         :message/text text
                         :message/channel-id channel
                         :message/user-id user
                         :message/system? true})

    (#{"me_message" "reminder_add"} subtype)
    (add-message message-tree {:message/timestamp ts
                         :message/text text
                         :message/channel-id channel
                         :message/user-id user
                         :message/emphasis? true})

    (= "slack_image" subtype)
    (add-message message-tree {:message/timestamp ts
                         :message/text text
                         :message/channel-id channel
                         :message/user-id user
                         :message/image? true})

    :else
    message-tree))

(defmethod add-event "reaction_added" [message-tree {:strs [ts reaction item]}]
  (if (contains? message-tree (get item "ts"))
    (update-in message-tree [(get item "ts") :message/reactions reaction] (fnil inc 0))
    message-tree))

(defmethod add-event "reaction_removed" [message-tree {:strs [ts reaction item]}]
  (if (contains? message-tree (get item "ts"))
    (update-in message-tree [(get item "ts") :message/reactions reaction] (fnil dec 0))
    message-tree))

;; Might still want to handle these later
(defmethod add-event "member_join_channel" [message-tree {:strs [ts user]}]
  message-tree)

(defmethod add-event "member_left_channel" [message-tree {:strs [ts user]}]
  message-tree)



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
