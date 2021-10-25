(ns co.gaiwan.slack.archive.files
  "Working with an archive as a set of files, partitioned by channel and day.

  An Archive in this sense is a directory which contains per-channel
  subdirectories, which in turn contain per-day json-lines files.

  ```
  ├── GQ54T7CCX
  │   └── 2020-06-16.jsonl
  └── GQ6JA3UBF
      ├── 2019-11-05.jsonl
      ├── 2020-05-29.jsonl
      └── 2020-06-23.jsonl
  ```

  When working with such an archive we have a `{:dir \"archive-directory\"}` map
  called arch, which may also contain other things to help incrementally
  construct the archive."
  (:require [clojure.java.io :as io]
            [co.gaiwan.slack.time-util :as time-util]
            [co.gaiwan.json-lines :as jsonl]
            [jsonista.core :as jsonista])
  (:import (java.io File)))

(defn log-files
  "Get a sequence of all slack log files in a directory, alphabetically sorted."
  [dir]
  (eduction
   (filter (complement (memfn ^File isDirectory)))
   (file-seq (io/file dir))))

(defn chan-day-file ^File [dir channel day]
  (io/file dir channel (str day ".jsonl")))

(defn arch-ensure-output-writer
  "Make sure there is an output writer that is suitable for appending an event
  with the given channel and timestamp. Returns a pair of [writer arch-map]."
  [{:keys [dir] :as arch} channel ts]
  (let [day (time-util/format-inst-day (time-util/ts->inst ts))
        file (chan-day-file dir channel day)]
    (if-let [writer (get-in arch [:writers channel day])]
      [writer arch]
      (do
        (.mkdirs (.getParentFile ^File file))
        (let [writer (io/writer file :append true)]
          [writer (assoc-in arch [:writers channel day] writer)])))))

(defn close-writers
  "Close all writers in the arch map."
  [arch]
  (doseq [[channel days] (:writers arch)
          [day writer] days]
    (.close ^java.io.Closeable writer))
  (dissoc arch :writers))

(defn dir-event-seq
  "Given a directory like the clojurians-log raw json archive, return a sequence
  of all the raw events. Takes about ~35 seconds for the 3.8M events of
  Clojurians, or about 2 seconds for the Devops Enterprise history"
  [dir]
  (eduction
   (mapcat jsonl/slurp-jsonl)
   (log-files dir)))

(defn- archive-append-event* [arch channel ts event]
  (let [[writer arch] (arch-ensure-output-writer arch channel ts)]
    (jsonl/jsonl-append writer event)
    arch))

(defn archive-append-event
  "Append a single slack event to the archive.
  Assuming the event has a channel and timestamp, it will be appended to the
  correct channel/day json-lines file."
  [arch {:strs [type subtype channel channel_id ts event_ts item edited thread_ts] :as event}]
  (let [channel (or (get item "channel") channel channel_id)
        timestamp (or (get edited "ts") (get item "ts") thread_ts ts event_ts)
        #_(cond-> #{}
            (or (get item "ts") thread_ts ts event_ts)
            (conj (or (get item "ts") thread_ts ts event_ts))
            (= "thread_broadcast" subtype)
            (conj ts))]
    ;; Skip DM/group events, only regular channels
    (if (and (string? channel) (= \C (first channel)))
      (if (= "thread_broadcast" subtype)
        ;; thread broadcast messages get added twice, once at the top level as a
        ;; standalone message, and once as a message reply. We change the
        ;; subtype in the second case so we can easily distinguish these. These
        ;; may end up on the same day, or on separate days.
        (-> arch
            (archive-append-event* channel ts event)
            (archive-append-event* channel thread_ts (assoc event "subtype" "message_replied")))
        (-> arch
            (archive-append-event* channel (or (get item "ts") thread_ts ts event_ts) event)))
      arch)))

(defn build-archive
  "Take a \"raw\" archive (pile of json-lines files, e.g. coming from rtmbot or
  slack-backfill) and convert them into an archive, separated by channel and
  day."
  [source dest]
  (let [arch {:dir dest}]
    (close-writers
     (reduce
      archive-append-event
      arch
      (dir-event-seq source)))))

(defn slurp-chan-day
  "Get the raw list of events for a given channel and day"
  [arch channel-id day-str]
  (let [f (chan-day-file (:dir arch) channel-id day-str)]
    (when (.exists f)
      (jsonl/slurp-jsonl f))))

(comment
  (time
   (build-archive
    "/home/arne/github/clojurians-log/logs"
    "/tmp/clojurians-archive"))
  ;; Takes about 132 seconds on my laptop
  (time
   (build-archive
    "/home/arne/ITRevolution/devopsenterprise-slack-archive/logs"
    "/tmp/gene-archive"))
  ;; 6.5 seconds
  (defmacro timecount [form]
    `(let [start# (System/nanoTime)
           cnt# (count ~form)
           end# (System/nanoTime)
           ms# (/ (- end# start#) 1e6)]
       (println (format "%d in %fms = %d/sec" (long cnt#) ms# (long (/ cnt# ms# 0.001))))
       cnt#))

  (timecount
   (slurp-chan-day {:dir "/tmp/gene-archive"}
                   "C0155U72JP9"
                   "2020-10-13"))

  (timecount
   (slurp-jsonl "/tmp/gene-archive/C0155U72JP9/2020-10-13.jsonl"))

  (set! *warn-on-reflection* true)

  (timecount
   )

  )
