(ns co.gaiwan.slack.raw-archive
  "A raw archive is an essentially unstructured pile of json-lines files,
  containing raw slack events, mostly coming from the RTM API, but in the case
  of backfills possibly coming from the web API.

  This namespace offers operations over such an archive, e.g. reading all of its
  events."
  (:require [clojure.string :as str]
            [co.gaiwan.json-lines :as jsonl]
            [clojure.java.io :as io])
  (:import (java.io File)))

(defn file-seq-by-exts
  "Get a sequence of all regular files in the directory with one of the given
  extensions. Returns a sequence of [[File]] instances."
  [dir exts]
  (eduction
   (comp
    (filter (complement (memfn ^File isDirectory)))
    (filter (fn [f] (some #(str/ends-with? (str f) %) exts))))
   (file-seq (io/file dir))))

(defn dir-event-seq
  "Given a directory like the clojurians-log raw json archive, return an eduction
  which yields of all the raw events. Takes about ~35 seconds to fully realize
  for the 3.8M events of Clojurians, or about 2 seconds for the Devops
  Enterprise history"
  ([dir]
   (dir-event-seq dir [".txt" ".jsonl"]))
  ([dir exts]
   (eduction
    (mapcat (fn [f] (map-indexed #(vary-meta %2 assoc :file f :line (inc ^long %1))
                                 (jsonl/slurp-jsonl f))))
    (file-seq-by-exts dir exts))))

(comment
  (require '[co.gaiwan.slack.profiling-util :refer :all])
  (timecount (seq (dir-event-seq "/home/arne/github/clojurians-log"))))
