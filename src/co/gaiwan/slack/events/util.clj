(ns co.gaiwan.slack.events.util
  (:require
   [clojure.string :as str]
   [co.gaiwan.slack.time-util :as time-util])
  (:import
   (java.time.format DateTimeFormatter)))

(defn micros->ts
  "Converts `micros` into a timestamp string."
  [micros]
  (->> micros
       (format "%016d")
       (re-find #"(\d{10})(\d{6})")
       rest
       (str/join \.)))

(defn micros->millis [micros]
  (long (/ micros 1e3)))

;; TODO: remove this in favor of raw-event/event-ts
(defn event-ts
  "Get the time the event happened, most events have a `event_ts`, the ones that
  don't generally have a `ts`. There are some exceptions for connect/disconnect
  messages, for those [[event-ts]] returns `nil`"
  [e]
  (get e "event_ts" (get e "ts")))

(defn event-ts-micros
  "Derives `event` time (in microseconds) from its timestamp."
  [event]
  (time-util/ts->micros (event-ts event)))

(def empty-event-set
  "Sorted set, any events added will be kept sorted in event_ts order."
  (sorted-set-by (fn [this that]
                   (compare (event-ts this)
                            (event-ts that)))))

(defn into-event-set
  "Add events into a sorted set, sorted by event_ts. Remove the few rare events
  that don't have a event_ts or ts."
  ([events]
   (into-event-set empty-event-set events))
  ([event-set events]
   (into event-set (filter event-ts) events))
  ([event-set xform events]
   (into event-set (comp (filter event-ts) xform) events)))

(defn format-evt
  "Naively format events in a human readable way, good for printing out messages
  when debugging."
  ([event max-length]
   (let [msg (format-evt event)]
     (str
      (subs msg 0 (min max-length (count msg)))
      (when (< max-length (count msg))
        "..."))))
  ([{:strs [type subtype user text reaction message]
     :as event}]
   (if-not (event-ts event)
     (prn-str event)
     (let [user (or user (get message "user"))
           text (some-> (or text (get message "text"))
                        (str/replace #"\R" "\\n"))]
       (str
        (time-util/format-debug (time-util/ts->inst (event-ts event)))
        " "
        (when user
          (if (map? user)
            (str "<" (get user "id") "> ")
            (str "<" user "> ")))
        "[" type
        (when subtype (str "/" subtype))
        "] "
        (when text
          text)
        (when reaction
          (str
           reaction
           " for " (get-in event ["item" "ts"]))))))))
