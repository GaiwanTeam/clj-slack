(ns repl-sessions.message-pipeline
  (:require [clojure.java.io :as io]
            [co.gaiwan.slack.raw-archive :as raw]
            [co.gaiwan.slack.archive :as archive]))


(def cljians-log-dir "/path/to/clojurians-log/logs")
;; 3.8M events

(def raw-events (raw/dir-event-seq cljians-log-dir))

(time (reduce (fn [i _] (inc i))
              0
              (raw/dir-event-seq cljians-log-dir)))

(time
 (def arch (archive/raw->archive cljians-log-dir (archive/archive "/tmp/cljians-archive"))))
(= (map #(get % "ts") (archive/slurp-chan-day-raw arch "C06MAR553" "2021-06-12"))
   (sort (map #(get % "ts") (archive/slurp-chan-day-raw arch "C06MAR553" "2021-06-12"))))
(archive/slurp-chan-day arch "C03S1L9DN" "2015-06-11")
(archive/slurp-chan-day-raw arch "C03S1KBA2" "2020-03-31")

(frequencies
 (keep (fn [e]
         (when-not (archive/event->channel e)
           (get e "type")))
       (raw/dir-event-seq cljians-log-dir)))

(take 100
      (filter (fn [e]
                (and (= "message" (get e "type"))
                     (not (archive/event->channel e))))
              (raw/dir-event-seq cljians-log-dir)))

(def events
  (filter (comp #{"1623231232.116500" "1623231684.117100"} #(get % "ts"))
          (raw/dir-event-seq cljians-log-dir)))
(take 10 (raw/dir-event-seq cljians-log-dir))

(def ch "C0E1SN0NM")

(filter #{"1623231232.116500" "1623231684.117100"} (get-in arch [:timestamps ch]))

(filter archive/default-event-filter-predicate events)

(set! *print-namespace-maps* false)

4204950
