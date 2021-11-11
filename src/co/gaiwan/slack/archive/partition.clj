(ns co.gaiwan.slack.archive.partition
  "Partition raw slack events into channel+day separated json-lines files."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [co.gaiwan.slack.time-util :as time-util]
            [co.gaiwan.json-lines :as jsonl]
            [jsonista.core :as jsonista])
  (:import (java.io File)
           (java.time ZoneId)))

(defn chan-day-file
  "Get the java.io.File containing the events for a given channel+day."
  ^File [dir channel day]
  (io/file dir channel (str day ".jsonl")))

(defn ensure-output-writer
  "Make sure there is an output writer that is suitable for appending an event
  with the given channel and timestamp. Returns a pair of [writer arch-map].

  Keeps open writers under the `:writers` key inside the arch map and reuses
  them, see [[close-writers]] for cleaning up when you're done."
  [{:keys [dir zone-id] :as arch} channel day-str]
  (let [file (chan-day-file dir channel day-str)]
    (if-let [writer (get-in arch [:writers channel day-str])]
      [writer arch]
      (do
        (.mkdirs (.getParentFile ^File file))
        (let [writer (io/writer file :append true)]
          [writer (assoc-in arch [:writers channel day-str] writer)])))))

(defn close-writers
  "Close all writers in the arch map.

  See also [[ensure-output-writer]]"
  [arch]
  (doseq [[channel days] (:writers arch)
          [day writer] days]
    (.close ^java.io.Closeable writer))
  (dissoc arch :writers))

(defn archive-append-event-channel-day
  "Append a single slack event to the archive, assuming you already know how to
  partition it, i.e. you have the channel and the timestamp by which it should
  be partitioned (which may not be the timestamp of the event, in the case of
  replies or reactions which span date boundaries)"
  [arch channel day-str event]
  (let [event-ts (get event "ts")]
    (if (get-in arch [:timestamps channel event-ts])
      arch
      (let [[writer arch] (ensure-output-writer arch channel day-str)]
        (jsonl/jsonl-append writer event)
        (update-in arch [:timestamps channel] (fnil conj #{}) event-ts)))))

(defn event-partition-channel
  "Given a raw event, determine how it should be partitioned, i.e. its channel and
  the timestamp which determines which day to add it to. This function
  determines the channel-id.

  This is meant to be able to handle most types of messages, despite their
  differences in how they encode channel and timestamp information. The main
  thing to note here is that if an event refers to another event (e.g. by having
  an `\"item\"`, `\"message\"`, or `\"edited\"` key), then it should be
  partitioned based on the timestamp of the event it points to, rather than its
  own timestamp.

  Any code that adds to the partitioned archive should use this logic, to ensure
  that all events necessary to render a given day all end up in the same place.

  See also [[event-partition-timestamp]]."
  [{:strs [channel channel_id item] :as event}]
  (or (get item "channel")
      channel
      channel_id))

(defn event-partition-timestamp
  "Given a raw event, determine how it should be partitioned, i.e. its channel and
  the timestamp which determines which day to add it to. This function
  determines the timestamp.

  This is meant to be able to handle most types of messages, despite their
  differences in how they encode channel and timestamp information. The main
  thing to note here is that if an event refers to another event (e.g. by having
  an `\"item\"`, `\"message\"`, or `\"edited\"` key), then it should be
  partitioned based on the timestamp of the event it points to, rather than its
  own timestamp.

  Any code that adds to the partitioned archive should use this logic, to ensure
  that all events necessary to render a given day all end up in the same place.

  See also [[event-partition-channel]]."
  [{:strs [ts event_ts item edited message thread_ts] :as event}]
  (or (get edited "ts")
      (get message "ts")
      (get item "ts")
      thread_ts
      ts
      event_ts))

(defn event-partition-day-str
  "Given an event, return a day-str like `\"2020-10-15\"`, which is the day it
  will be appended to. This is the default implementation, this function can be
  injected to change the logic, e.g. to accomodate for time zones."
  [event]
  (time-util/format-inst-day (time-util/ts->inst (event-partition-timestamp event))))

(defn event->day-tz
  "Return an `event->day` function which allocates events to dates based on a
  specific timezone."
  [tz]
  (let [formatter (.withZone time-util/inst-day-formatter (ZoneId/of tz))]
    (fn [event]
      (.format formatter (time-util/ts->inst (event-partition-timestamp event))))))

(defn archive-append-event
  "Append a single slack event to the archive.
  Assuming the event has a channel and timestamp, it will be appended to the
  correct channel/day json-lines file."
  [arch {:strs [subtype channel ts thread_ts] :as event} {:keys [event->channel-id event->day]
                                                          :or {event->channel-id event-partition-channel
                                                               event->day event-partition-day-str}}]
  (let [channel (event->channel-id event)
        day-str (event->day event)]
    (if (= "thread_broadcast" subtype)
      ;; thread broadcast messages get added twice, once at the top level as a
      ;; standalone message, and once as a message reply. We change the
      ;; subtype in the second case so we can easily distinguish these. These
      ;; may end up on the same day, or on separate days.
      (-> arch
          (archive-append-event-channel-day channel ts event)
          (archive-append-event-channel-day channel thread_ts (assoc event "subtype" "message_replied")))
      (-> arch
          (archive-append-event-channel-day channel day-str event)))))

(defn into-archive
  "Add raw events to the archive, partitioning them in the process. Takes a
  seqable of raw events. Will prevent duplicates within a single run by keeping
  a set of processed timestamps per channel.

  Optionally takes an `xform` transducer to filter or transform the messages
  before they get handled. See [[default-event-filter-predicate]]
  and [[co.gaiwan.slack.archive/build-archive]].

  See [[co.gaiwan.slack.raw-archive/dir-event-seq]] for what to feed into this
  function.

  The options map understands the keys `:event->channel-id`, and `:event->day`,
  two functions which take a raw event (map with string keys) and return a
  string, a channel id and day-str (e.g. `\"2020-10-15\"`) respectively. Default
  implementations are provided which allocate messages based on their UTC date."
  ([arch events]
   (into-archive arch identity events))
  ([arch xform events]
   (into-archive arch xform events nil))
  ([arch xform events opts]
   (close-writers
    (transduce
     xform
     (completing
      (fn [arch event]
        (archive-append-event arch event opts)))
     arch
     events))))
