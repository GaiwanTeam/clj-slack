(ns repl-sessions.ingest
  "Benchmarking to compare various line-based data formats, e.g. edn-lines,
  json-lines, transit-lines. Line-based so we can easily append events.

  Conclusion: json-lines with Jsonista/jackson is by far the fastest, which is
  great cause that is also our source format. (what comes out of rtmbot.)"
  (:require #_[lambdaisland.edn-lines :as ednl]
            [charred.api :as charred]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [cognitect.transit :as transit]
            [jsonista.core :as jsonista])
  (:import (java.io File PrintWriter PushbackReader StringWriter
                    Writer StringReader EOFException ByteArrayInputStream ByteArrayOutputStream)
           (java.util LinkedList)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* :warn-on-boxed)

(def does-log-dir "/home/arne/ITRevolution/devopsenterprise-slack-archive/logs")
;; 250k events
(def cljians-log-dir "/home/arne/github/clojurians-log/logs")
;; 3.8M events

(defn log-file-seq [dir]
  (sort-by str
           (filter (complement (memfn ^File isDirectory))
                   (file-seq (io/file dir)))))

(def does-logs (log-file-seq does-log-dir))
(def cljians-logs (log-file-seq cljians-log-dir))

(defn slurp-jsonl [file]
  (map json/read-str (line-seq (io/reader file))))

(defn spit-jsonl [f contents & [{:keys [append?] :or {append? false} :as options}]]
  (with-open [out (io/writer (io/file f) :append append?)]
    (run! #(do
             (json/write-json % out false)
             (.append out \newline)) contents)))

(def jsonista-mapper
  (jsonista/object-mapper
   {:factory
    (doto (com.fasterxml.jackson.core.JsonFactory.)
      (.disable com.fasterxml.jackson.core.JsonGenerator$Feature/AUTO_CLOSE_TARGET))}))

(defn slurp-jsonl2 [file]
  (map #(jsonista/read-value % jsonista-mapper) (line-seq (io/reader file))))

(defn spit-jsonl2 [f contents & [{:keys [append?] :or {append? false} :as options}]]
  (with-open [out (io/writer (io/file f) :append append?)]
    (run! #(do
             (jsonista/write-value out % jsonista-mapper)
             (.append out \newline)) contents)))

(defn slurp-transit [file]
  ;; Read data from a stream
  (let [reader (transit/reader (io/input-stream file) :json)]
    (transit/read reader)))

(defn spit-transit [file contents]
  (with-open [out (io/output-stream (io/file file))]
    (let [tw (transit/writer out :json)]
      (transit/write tw contents))))

(defn slurp-transit-lines [file]
  (let [reader (transit/reader (io/input-stream file) :json)]
    (loop [result (transient [])]
      (let [val (try
                  (transit/read reader)
                  (catch Exception _
                    ::eof))]
        (if (= ::eof val)
          (persistent! result)
          (recur (conj! result val)))))))

(defn spit-transit-lines [file contents]
  (with-open [out (io/output-stream (io/file file))]
    (let [tw (transit/writer out :json)]
      (run! #(do
               (transit/write tw %)
               (.write out 10))
            contents))))

(defn slurp-jsonl3 [file]
  #_(map #(charred/read-json %) (line-seq (io/reader file)))
  (let [r (slurp file)
        s (charred/read-json-supplier r {:eof-value ::eof
                                         :eof-error? false
                                         :bufsize 2024})]
    (loop [res (transient [])]
      (let [v (.get s)]
        (if (= ::eof v)
          (persistent! res)
          (recur (conj! res v)))))))

(timecount (slurp-jsonl "/tmp/100k.jsonl"))
(timecount (slurp-jsonl2 "/tmp/100k.jsonl"))
(timecount (slurp-jsonl3 "/tmp/100k.jsonl"))

(defmacro timecount [form]
  `(let [start# (System/nanoTime)
         cnt# (count ~form)
         end# (System/nanoTime)
         ms# (/ (- end# start#) 1e6)]
     (println (format "%d in %fms = %d/sec" (long cnt#) ms# (long (/ cnt# ms# 0.001))))
     cnt#))

(timecount
 (mapcat slurp-jsonl does-logs))

(time
 (count
  (mapcat
   (comp
    (partial pmap json/read-json)
    line-seq
    io/reader)
   (filter (complement (memfn ^File isDirectory))
           (file-seq (io/file does-logs))))))

;; 50_000 records / second
;; pmap can shave off ~20% (on 4 cores)

(def xxx (first
          (map #(PushbackReader. (io/reader %) 64)
               (filter (complement (memfn isDirectory))
                       (file-seq (io/file does-logs))))))
(first (filter (complement (memfn isDirectory))
               (file-seq (io/file does-logs))))

(defn read-json-lines [file]
  (let [reader (PushbackReader. (io/reader file) 1024)
        result (LinkedList.)]
    (loop []
      (let [el (#'json/-read reader false ::eof json/default-read-options)]
        (if (identical? el ::eof)
          result
          (do
            (.add result el)
            (recur)))))))

(count
 (read-json-lines (first (filter (complement (memfn ^File isDirectory))
                                 (file-seq (io/file does-logs))))))
(time
 (count
  (mapcat read-json-lines
          (filter (complement (memfn ^File isDirectory))
                  (file-seq (io/file does-logs))))))

(transduce (comp
            (filter (complement (memfn ^File isDirectory)))
            (mapcat read-json-lines))
           (fn
             ([x] x)
             ([x y] (inc ^long x)))
           0
           (file-seq (io/file does-logs)))

(def hundred-k
  (take 100000 (mapcat read-json-lines does-logs)))

(count hundred-k)

(time (ednl/spit "/tmp/100k.ednl" hundred-k))
(time (spit-jsonl "/tmp/100k.jsonl" hundred-k))
(time (spit-jsonl2 "/tmp/100k.jsonl" hundred-k))
(time (spit-transit "/tmp/100k.transit" hundred-k))
(time (spit-transit-lines "/tmp/100k.transitl" hundred-k))

;; edln: write 11k events/second
;; jsonl: write 50k events/second
;; jsonl2: write 100k events/second
;; transit: write 10k events/second
;; transit-lines: write 10k events/second

(timecount (ednl/slurp "/tmp/100k.ednl"))
(timecount (slurp-jsonl "/tmp/100k.jsonl"))
(timecount (slurp-jsonl2 "/tmp/100k.jsonl"))
(timecount (slurp-jsonl3 "/tmp/100k.jsonl"))
(timecount (slurp-jsonl2 "/tmp/gene-archive/C0155U72JP9/2020-10-13.jsonl"))

(timecount (slurp-transit "/tmp/100k.transit"))
(timecount (slurp-transit-lines "/tmp/100k.transitl"))

;; ednl: read 19k events/second
;; json: read 45k events/second
;; transit: read 81k events/second
;; transit-lines: read 92k events/second
;; jsonl2: read 105k events/second

(do
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C0155U72JP9/2020-10-13.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C0155U72JP9/2020-06-23.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C0155U72JP9/2021-10-05.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C0155U72JP9/2020-10-15.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C015DQFEGMT/2020-06-25.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C015DQFEGMT/2020-10-13.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C015DQFEGMT/2020-10-15.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C0155U72JP9/2021-05-18.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C015DQFEGMT/2021-10-06.jsonl"))
  (timecount (slurp-jsonl2 "/tmp/gene-archive/C015DQFEGMT/2020-06-24.jsonl"))
  )

;; These are the biggest archive files for does, the first one takes 190ms, the
;; others 30-40ms. These are outliers though, the median archive file is under
;; 1kB, the vast majority can be read in under 1ms.

(timecount (slurp-jsonl2 "/tmp/gene-archive/C01CJH7P3M2/2021-10-04.jsonl"))
(timecount (slurp-jsonl2 "/tmp/gene-archive/C01TK0K6QU9/2021-10-04.jsonl"))
(timecount (slurp-jsonl2 "/tmp/gene-archive/C01C6EV0ZC6/2021-05-20.jsonl"))

(double (/ 100000 9567.85 ))

(remove (comp #{"user_change" "team_join" "dnd_updated_user" "goodbye"}  #(get % "type"))
        (remove (fn [{:strs [ts channel item channel_id event_ts]}]
                  (or item
                      (and ts channel)
                      (and channel_id event_ts)
                      (and channel event_ts)))
                hundred-k))
(filter #(#{"message"} (get % "type")))
