(ns co.gaiwan.slack.archive.files
  "Working with an archive as a set of files, partitioned by channel and day.

  An Archive in this sense is a directory which contains per-channel
  subdirectories, which in turn contain per-day json-lines files.

  When working with such an archive we have a `{:dir \"archive-directory\"}` map
  called arch, which may also contain other things to help incrementally
  construct the archive."
  (:require [clojure.java.io :as io]
            [co.gaiwan.slack.time-util :as time-util]
            [jsonista.core :as jsonista])
  (:import (java.io File)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def jsonista-mapper
  (jsonista/object-mapper
   {:factory
    (doto (com.fasterxml.jackson.core.JsonFactory.)
      (.disable com.fasterxml.jackson.core.JsonGenerator$Feature/AUTO_CLOSE_TARGET))}))

(defn jsonl-append
  "Append a single value to a json-lines file/writer (newline delineated json)"
  [out value]
  (jsonista/write-value out value jsonista-mapper)
  (.append ^java.io.Writer out \newline)
  nil)

(defn slurp-jsonl
  "Slurp json-lines (newline delineated json)"
  [file]
  (map #(jsonista/read-value % jsonista-mapper) (line-seq (io/reader file))))

(defn spit-jsonl
  "Write out json-lines (newline delineated json)"
  [f contents & [{:keys [append?] :or {append? false} :as options}]]
  (with-open [out (io/writer (io/file f) :append append?)]
    (run! jsonl-append contents)))

(defn log-file-seq
  "Get a sequence of all slack log files in a directory, alphabetically sorted."
  [dir]
  (sort-by str
           (filter (complement (memfn ^File isDirectory))
                   (file-seq (io/file dir)))))

(defn arch-ensure-output-writer
  "Make sure there is an output writer that is suitable for appending an event
  with the given channel and timestamp. Returns a pair of [writer arch-map]."
  [{:keys [dir] :as arch} channel ts]
  (let [day (time-util/format-inst-day (time-util/ts->inst ts))
        file (io/file dir channel (str day ".jsonl"))]
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
  (mapcat slurp-jsonl (log-file-seq dir)))

(defn archive-append-event* [arch channel ts event]
  (let [[writer arch] (arch-ensure-output-writer arch channel ts)]
    (jsonl-append writer event)
    arch))

(defn archive-append-event [arch {:strs [type subtype channel channel_id ts event_ts item thread_ts] :as event}]
  (let [channel (or (get item "channel") channel channel_id)
        timestamps (cond-> #{}
                     (or (get item "ts") thread_ts ts event_ts)
                     (conj (or (get item "ts") thread_ts ts event_ts))
                     (= "thread_broadcast" subtype)
                     (conj ts))]
    (if (string? channel)
      (reduce (fn [arch ts]
                (archive-append-event* arch channel ts event))
              arch
              timestamps)
      arch)))

(defn build-archive [source dest]
  (let [arch {:dir dest}]
    (close-writers
     (reduce
      archive-append-event
      arch
      (dir-event-seq source)))))

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
  )
