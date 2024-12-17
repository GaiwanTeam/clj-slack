(ns co.gaiwan.slack.raw-event
  "API for working with individual raw slack events"
  (:refer-clojure :exclude [type]))

(defn type [{:strs [type]}] type)
(defn subtype [{:strs [subtype]}] subtype)
(defn user-id [{:strs [user bot_id]}] (or user bot_id))

(defn event-ts
  "Get the time the event happened, most events have a `event_ts`, the ones that
  don't generally have a `ts`. There are some exceptions for connect/disconnect
  messages, for those [[event-ts]] returns `nil`"
  [e]
  (get e "event_ts" (get e "ts")))

(defn message-ts
  "Which message does this event pertain to? This differs from message type to
  message type. This function handles replies like regular messages, so you get
  the message-ts of the reply, not of the parent. See [[parent-ts]] for an
  alternative.

  This may not be exhaustive yet, we always fall back to returning the `ts` of
  the event if no special case applies.

  - reaction_added/reaction_removed : the message the user is reacting to
  - message_deleted/tombstone: the message that was deleted
  - message_changed : the message that was edited
  - pin_added / removed: the message that was pinned
  "
  [{:strs [type subtype ts item message deleted_ts]}]
  (case type
    "reaction_added"
    (get item "ts")
    "reaction_removed"
    (get item "ts")
    "pin_added"
    (get-in item ["message" "ts"])
    "pin_removed"
    (get-in item ["message" "ts"])
    "message"
    (case subtype
      "message_changed"
      (get message "ts")
      "message_deleted"
      deleted_ts
      ts)
    ts))

(defn parent-ts
  "Get the timestamp of the message that this is a reply to. Returns nil if it's
  not a reply."
  [{:strs [thread_ts ts]}]
  (when (not= thread_ts ts)
    thread_ts))

(defn regular-message?
  "Does this event add a regular message to the channel? This could be a plain
  message, a reply, or a message by a bot."
  [{:strs [type subtype]}]
  (and (= "message" type)
       (or (nil? subtype)
           ;; the message_replied subtype is used inconsistently by
           ;; Slack, depending on how we get the event (API vs RTM) this
           ;; subtype may or may not be present, the reliable way to
           ;; check for a reply is to look for the presence of
           ;; thread_ts, and to confirm that it's different from the
           ;; parent ts
           (= "message_replied" subtype)
           (= "bot_message" subtype)
           (= "thread_broadcast" subtype))))

(defn system-message? [{:strs [subtype]}]
  (#{"channel_archive"
     "channel_join"
     "channel_name"
     "channel_purpose"
     "channel_topic"} subtype))

(defn reply?
  "Is this event a message event for a message that is a reply to a previous
  message. There are a few subtleties to deal with here:

  - the subtype may be `nil`, `\"message_replied\"`, or `\"thread_broadcast`,
    Slack acknowledges this inconsistency, see
    <https://api.slack.com/events/message/message_replied>
  - So we need to check for `thread_ts`, but when requesting events from the API
    the parent of a thread will also have a `thread_ts`, equal to its own `ts`
  "
  [{:strs [type subtype ts thread_ts]}]
  (and (= "message" type)
       (or (nil? subtype)
           (= "thread_broadcast" subtype)
           (= "message_replied" subtype))
       thread_ts
       (not= ts thread_ts)))

(defn thread-broadcast?
  "Is this a reply that also gets sent to the channel itself?"
  [{:strs [type subtype]}]
  (and (= "message" type)
       (= "thread_broadcast" subtype)))

(defn channel-id
  "Get the channel this message applies to, if any."
  [{:strs [channel channel_id item]}]
  (or channel
      channel_id
      (get item "channel")))
