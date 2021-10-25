(ns co.gaiwan.json-lines
  "Handle json-lines files (also known as newline-delimited JSON)"
  (:require [clojure.java.io :as io]
            [jsonista.core :as jsonista])
  (:import java.io.File))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def jackson-factory
  (doto (com.fasterxml.jackson.core.JsonFactory.)
    (.disable com.fasterxml.jackson.core.JsonGenerator$Feature/AUTO_CLOSE_TARGET)
    (.disable com.fasterxml.jackson.core.JsonParser$Feature/AUTO_CLOSE_SOURCE)))

(def jsonista-mapper (jsonista/object-mapper {:factory jackson-factory}))
(def jsonista-mapper-keywordize (jsonista/object-mapper {:factory jackson-factory :decode-key-fn true}))

(defn jsonl-append
  "Append a single value to a json-lines file/writer (newline delineated json)"
  [out value]
  (jsonista/write-value out value jsonista-mapper)
  (.append ^java.io.Writer out \newline)
  nil)

(defn slurp-jsonl
  "Slurp json-lines (newline delineated json)"
  ([file]
   (slurp-jsonl file jsonista-mapper))
  ([^File file ^com.fasterxml.jackson.databind.ObjectMapper mapper]
   (with-open [f (io/input-stream file)]
     (-> mapper
         (.readerFor com.fasterxml.jackson.databind.JsonNode)
         (.withType Object)
         (.readValues f)
         iterator-seq
         doall))))

(defn slurp-keywordized [^File file]
  (slurp-jsonl file jsonista-mapper-keywordize))

(defn spit-jsonl
  "Write out json-lines (newline delineated json)"
  [f contents & [{:keys [append?] :or {append? false} :as options}]]
  (with-open [out (io/writer (io/file f) :append append?)]
    (run! (partial jsonl-append out) contents)))
