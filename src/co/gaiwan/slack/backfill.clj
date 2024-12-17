(ns co.gaiwan.slack.backfill
  "Fetch the recent Slack history via the API

  Used when bootstrapping a new archive, or when the real time message capturing
  has been down

  Run with

  clj -X:run/backfill :token '\"xoxb-....\"' :output-dir '\"/tmp/backfill\"'
  "
  (:require
   [co.gaiwan.slack.api :as api]
   [clojure.java.io :as io]
   [charred.api :as json]))

(defn fetch-logs [conn target-dir]
  (doseq [{:keys [name id] :as channel} (api/conversations conn)]
    (println "Fetching" id (str "#" name))
    (let [history (api/history conn {:channel id})]
      (when (seq history)
        (with-open [file (io/writer (io/file target-dir (str "000_" id "_" name ".txt")))]
          (doseq [message history]
            (json/write-json message file)
            (.write file "\n")
            (when (:thread_ts message)
              (doseq [message (api/replies conn {"channel" id "ts" (:ts message)})]
                (json/write-json message file)
                (.write file "\n")))))))))

(defn backfill [{:keys [token output-dir]}]
  (assert token "Missing :token")
  (assert output-dir "Missing :output-dir")
  (.mkdirs (io/file (str output-dir)))
  (fetch-logs (api/conn (str token)) (str output-dir)))
