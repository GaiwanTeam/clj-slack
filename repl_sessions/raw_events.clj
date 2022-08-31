(ns raw-events
  "Exploration of what Slack's \"raw\" events look like. This is a namespace that
  we will be revisiting often whenever we need to figure out how to deal with a
  specific type of incoming data. On the other hand this is the kind of thing
  that clj-slack should pave over, so that we get predictable Clojure values to
  work with.

  See also [[docs/data_structures.org]] and [[docs/architectural_decision_log.org]]."
  (:require [clojure.set :as set]
            [co.gaiwan.slack.raw-archive :as raw]
            [co.gaiwan.slack.time-util :as time-util]))

(def raw-slack-log "/home/arne/ITRevolution/devopsenterprise-slack-archive")

(defn event-type [e] (get e "type"))
(defn subtype [e] (get e "subtype"))

;; Which keys are present on all events? What is the common denominator?
(reduce
 #(filter (set %1) (set %2))
 (eduction
  (comp
   (take 100000)
   (map keys))
  (raw/dir-event-seq raw-slack-log)))
;; => ("type")

;; Turns out that only `"type"` is present on every event, what else is there is
;; dependent on the message type.
;;
;; Let's see which keys can we expect to find for a given event type:

(reduce
 (fn [m e]
   (update m (get e "type") (fnil set/intersection (set (keys e))) (set (keys e))))
 {}
 (raw/dir-event-seq raw-slack-log))

;; Result
(def mandatory-keys
  {"channel_joined" #{"type" "channel"},
   "message" #{"ts" "type"},
   "pin_added" #{"event_ts" "item_user" "channel_id" "user" "pin_count" "item" "type"},
   "desktop_notification" #{"event_ts" "avatarImage" "msg" "ssbFilename" "imageUri" "is_shared" "subtitle" "content" "launchUri" "title" "type" "channel"},
   "team_plan_change" #{"event_ts" "can_add_ura" "type" "plan"},
   "bot_added" #{"bot" "event_ts" "cache_ts" "type"},
   "file_created" #{"user_id" "event_ts" "file_id" "type" "file"},
   "channel_history_changed" #{"event_ts" "ts" "type" "channel" "latest"},
   "channel_archive" #{"event_ts" "is_moved" "user" "type" "channel"},
   "im_created" #{"event_ts" "user" "type" "channel"},
   "goodbye" #{"type"},
   "channel_left" #{"event_ts" "type" "channel"},
   "bot_changed" #{"bot" "event_ts" "cache_ts" "type"},
   "file_comment_edited" #{"user_id" "event_ts" "file_id" "type" "file" "comment"},
   "pref_change" #{"event_ts" "name" "value" "type"},
   "error" #{"error" "type"},
   "file_comment_added" #{"user_id" "event_ts" "file_id" "type" "file" "comment"},
   "apps_changed" #{"event_ts" "type" "app"},
   "user_change" #{"event_ts" "user" "type"},
   "channel_unarchive" #{"event_ts" "user" "type" "channel"},
   "reaction_added" #{"event_ts" "user" "reaction" "item" "type"},
   "channel_rename" #{"event_ts" "type" "channel"},
   "file_change" #{"user_id" "event_ts" "file_id" "type" "file"},
   "file_deleted" #{"event_ts" "file_id" "type"},
   "team_pref_change" #{"event_ts" "name" "value" "type"},
   "group_deleted" #{"event_ts" "type" "channel"},
   "file_comment_deleted" #{"user_id" "event_ts" "file_id" "type" "file" "comment"},
   "channel_created" #{"event_ts" "type" "channel"},
   "mobile_in_app_notification" #{"author_display_name" "event_ts" "push_id" "ts" "avatarImage" "author_id" "is_shared" "msg_text" "subtitle" "title" "type" "mobileLaunchUri" "channel" "notif_id" "channel_name"},
   "reaction_removed" #{"event_ts" "user" "reaction" "item" "type"},
   "team_join" #{"event_ts" "user" "cache_ts" "type"},
   "member_left_channel" #{"event_ts" "ts" "user" "type" "channel_type" "channel" "team"},
   "apps_installed" #{"event_ts" "type" "app"},
   "pin_removed" #{"event_ts" "item_user" "channel_id" "user" "pin_count" "item" "type" "has_pins"},
   "file_unshared" #{"user_id" "event_ts" "file_id" "channel_id" "type" "file"},
   "emoji_changed" #{"subtype" "event_ts" "type"},
   "app_actions_updated" #{"event_ts" "type" "is_uninstall" "app_id"},
   "benchmark" #{"lt" "id" "a" "type" "pct" "m"},
   "file_public" #{"user_id" "event_ts" "file_id" "type" "file"},
   "commands_changed" #{"event_ts" "commands_removed" "commands_updated" "type"},
   "file_shared" #{"user_id" "event_ts" "file_id" "type" "file"},
   "channel_deleted" #{"event_ts" "type" "channel"},
   "dnd_updated_user" #{"event_ts" "user" "type" "dnd_status"},
   "apps_uninstalled" #{"event_ts" "type" "app_id"},
   "member_joined_channel" #{"event_ts" "ts" "user" "type" "channel_type" "channel" "team"}})

;; Which event types are the most common:
(->> raw-slack-log
     raw/dir-event-seq
     (eduction (map event-type))
     frequencies
     set/map-invert
     (into (sorted-map)))

{1 "mobile_in_app_notification",
 2 "benchmark",
 3 "commands_changed",
 5 "app_actions_updated",
 6 "file_comment_deleted",
 10 "channel_archive",
 15 "apps_uninstalled",
 16 "apps_installed",
 19 "team_pref_change",
 20 "desktop_notification",
 28 "channel_rename",
 33 "channel_deleted",
 38 "error",
 42 "im_created",
 45 "file_comment_edited",
 55 "apps_changed",
 70 "bot_changed",
 105 "channel_joined",
 109 "bot_added",
 162 "emoji_changed",
 213 "file_comment_added",
 244 "pin_removed",
 321 "channel_created",
 326 "pin_added",
 360 "file_created",
 1241 "file_unshared",
 2315 "goodbye",
 2623 "file_deleted",
 3031 "file_change",
 4096 "reaction_removed",
 9347 "team_join",
 16317 "member_left_channel",
 21299 "file_public",
 21885 "file_shared",
 37012 "dnd_updated_user",
 53012 "user_change",
 129474 "member_joined_channel",
 159934 "reaction_added",
 3292879 "message"}

;; Which keys are the most common, considering that some events occur more
;; ofthen than others:

(->> raw-slack-log
     raw/dir-event-seq
     (eduction (mapcat keys))
     frequencies
     set/map-invert
     (into (sorted-map)))

;; Event keys by their total frequency in the clojurians-log data

{1 "channel_name",
 2 "m",
 3 "new_name",
 5 "is_uninstall",
 6 "pinned_to",
 9 "is_channel_invite",
 10 "is_moved",
 14 "collection_id",
 17 "is_auto_split",
 20 "app_id",
 21 "title",
 22 "names",
 27 "actor_id",
 29 "bot_link",
 34 "members",
 38 "error",
 48 "upload_reply_to",
 71 "app",
 160 "value",
 171 "purpose",
 176 "name",
 179 "bot",
 206 "icons",
 244 "has_pins",
 246 "new_broadcast",
 250 "plain_text",
 270 "item_type",
 275 "is_thread_broadcast",
 342 "slog_is_shared",
 349 "is_multiteam",
 512 "is_intro",
 569 "pinned_info",
 570 "pin_count",
 646 "topic",
 873 "comment",
 920 "reply_broadcast",
 1975 "silent",
 2295 "source",
 2452 "edited",
 2542 "bot_profile",
 2618 "channel_ids",
 2649 "is_locked",
 2674 "reactions",
 2788 "root",
 3268 "reply_count",
 3303 "inviter",
 7522 "parent_user_id",
 9607 "files",
 12185 "username",
 19035 "display_as_bot",
 19346 "upload",
 21081 "channel_id",
 25905 "attachments",
 31467 "deleted_ts",
 34357 "bot_id",
 37012 "dnd_status",
 46486 "cache_ts",
 48080 "user_id",
 50703 "file_id",
 58504 "file",
 123572 "user_profile",
 145801 "channel_type",
 162664 "item_user",
 164030 "reaction",
 164620 "item",
 369066 "thread_ts",
 552849 "previous_message",
 561298 "blocks",
 670920 "user_team",
 711622 "suppress_notification",
 923402 "message",
 954875 "hidden",
 978178 "client_msg_id",
 1093205 "source_team",
 1241900 "subtype",
 2338010 "text",
 2419636 "team",
 2552895 "event_ts",
 2723444 "user",
 3425279 "channel",
 3625183 "ts",
 3756723 "type"}

;; Now we can look at individual message types. This kind of analysis is
;; important because we have historical data going back to 2015. Some of these
;; event types have evolved over time, so a message event from 2015 may have
;; different keys than a message event from 2021. On top of that we combine data
;; from the RTM (real-time messaging) API, captured by the rtmbot, and from the
;; Web API, captured by the slack-backfill. These tend to largely follow the
;; same structure, but one may have associated data available in the event which
;; is absent in the other case, and needs to be resolved separately.

;; We'll ignore the keys that are present on all messages of a given
;; type (see [[mandatory-keys]]), so we can clearly see which optional keys show
;; up, and how often.

(defn event-type-key-frequencies [type]
  (->> raw-slack-log
       raw/dir-event-seq
       (eduction
        (filter (comp #{type} event-type))
        (map (fn [e] (remove (get mandatory-keys type) (keys e)))))
       frequencies
       set/map-invert
       (into (sorted-map))))

(event-type-key-frequencies "reaction_added")
;;=>
{221 ("item_user")
 1863 ("ts")
 157850 ("item_user" "ts")}

;; Here we see that while most `reaction_added` events have an `"item_user"` and a
;; `"ts"`, both are optional, and if possible we should write code that does not
;; rely on them.

;; At this point it gets interesting to be able to look at a few events to see
;; what the data looks like in practice. We'll write a function which returns a
;; sample, based on a transducer, so we can easily pass in custom filtering.

(defn sample-messages
  ([type]
   (sample-messages type identity))
  ([type xform]
   (->> raw-slack-log
        raw/dir-event-seq
        (sequence
         (comp
          (filter (comp #{type} event-type))
          xform
          (take 10000)))
        shuffle
        (take 5))))

(sample-messages "message" (comp (remove #(get % "thread_ts"))
                                 (remove subtype)))
(sample-messages "message" (comp
                            (filter (comp #{"bot_message"} subtype))))
(sample-messages "message" (comp
                            (filter (comp #{"tombstone"} subtype))))
(sample-messages "message" (comp
                            (filter (comp #{"message_deleted"} subtype))))

(sample-messages "message" (comp
                            (remove #(get % "attachments"))
                            (remove #(get % "files"))
                            (filter (comp #{"message_changed"} subtype))))
(sort-by #(get % "ts")
         (sample-messages "message" (comp
                                     (filter (comp #{"1633638135.029600"}
                                                   #(get % "thread_ts" (get % "ts")))))))

(sample-messages "message" (comp
                            (filter (comp #{"1621543244.008400",}
                                          #(get % "ts")))))

(sample-messages "reaction_added")
;; Seems `item_user` is the user-id of the user that posted the message that the
;; reaction was added to.
;;
;; Let's see what these events look like that don't have an `item_user`
(sample-messages "reaction_added" (remove #(get % "item_user")))

;; Nothing really sticks out, maybe some of them are from the web API?
(sample-messages "reaction_added" (comp
                                   (remove #(get % "item_user"))
                                   (map #(assoc % :file (:file (meta %))))))

;; No, that doesn't seem to be the case either. We could dig deeper and look at
;; the specific items that are being reacted to, maybe they are not regular
;; messages but some system event that doesn't have an associated user. In any
;; case this was just an example, we are not relying on `item_user`, and it
;; seems like without further investigation we shouldn't.

;; For messages there is a separate `"subtype"`, which is present on everything
;; that isn't a plain message send event, e.g. edit, delete, channel topic changes,
;; etc.

;; Let's see what keys we can expect on specific subtypes of messages

(reduce
 (fn [m e]
   (update m (subtype e)
           (fnil set/intersection (->> e
                                       keys
                                       (remove (conj (get mandatory-keys "message") "subtype"))
                                       set))
           (set (keys e))))
 {}
 (eduction
  (filter (comp #{"message"} event-type))
  (raw/dir-event-seq raw-slack-log)))

(get mandatory-keys "message");; => #{"ts" "type"}

;; Keys that appear in all messages of a given subtype, besides "ts", "type", "subtype":

(def subtype-mandatory-keys
  {nil #{"user" "text"},
   "app_conversation_leave" #{"user_profile" "event_ts" "inviter" "user" "text" "channel" "team"},
   "message_deleted" #{"event_ts" "hidden" "deleted_ts" "channel"},
   "reply_broadcast" #{"user" "timestamp" "broadcast_thread_ts" "text" "channel" "attachments" "is_multiteam"},
   "bot_message" #{"text"},
   "slackbot_response" #{"event_ts" "user" "text" "channel"},
   "message_changed" #{"message" "event_ts" "hidden" "channel"},
   "channel_archive" #{"user" "text" "channel"},
   "tombstone" #{"latest_reply" "reply_users" "subscribed" "user" "text" "thread_ts" "reply_users_count" "reply_count" "hidden" "parent_user_id" "is_locked"},
   "channel_join" #{"user" "text"},
   "file_share" #{"upload" "user" "text" "channel" "file"},
   "bot_disable" #{"user" "text" "bot_id" "channel"},
   "channel_purpose" #{"user" "text" "purpose"},
   "bot_add" #{"user" "text" "bot_id" "channel"},
   "channel_topic" #{"user" "text" "topic"},
   "message_replied" #{"message" "event_ts" "hidden" "channel"},
   "bot_enable" #{"user" "text" "bot_id" "channel"},
   "file_comment" #{"text" "channel" "file" "comment"},
   "app_conversation_join" #{"user_profile" "event_ts" "inviter" "user" "text" "channel" "team"},
   "thread_broadcast" #{"user" "root" "text" "thread_ts"},
   "file_mention" #{"user" "text" "channel" "file"},
   "reminder_add" #{"user" "text" "channel" "team"},
   "me_message" #{"user" "text"},
   "pinned_item" #{"user" "item_type" "text" "channel"},
   "bot_remove" #{"user" "text" "bot_id" "channel"},
   "channel_leave" #{"user" "text" "channel"},
   "channel_name" #{"user" "old_name" "name" "text"}})

(map (juxt meta #(select-keys % (concat
                                 (get mandatory-keys "message")
                                 (get subtype-mandatory-keys "message_replied")
                                 ["subtype" "text"]))))
(map #(get % "text")
     (sample-messages "message" (filter (comp #{"reply_broadcast"} subtype))))
[(sample-messages "message" (filter (comp #{"1614822402.022400"} #(get % "ts"))))
 (sample-messages "message" (filter (comp #{"1614822402.022400"} #(get % "thread_ts"))))]
