(ns co.gaiwan.slack.backfill
  "Fetch the recent Slack history via the API

  Used when bootstrapping a new archive, or when the real time message capturing
  has been down

  Run with

  clj -X:run/backfill :token '\"xoxb-....\"' :output-dir '\"/tmp/backfill\"'
  "
  (:require
   [charred.api :as json]
   [clojure.java.io :as io]
   [co.gaiwan.slack.api :as api]
   [co.gaiwan.slack.raw-event :as event]
   [co.gaiwan.slack.time-util :as time-util]
   [lambdaisland.cli :as cli]
   [lambdaisland.config :as config]
   [lambdaisland.config.cli :as config-cli])
  (:import
   (charred JSONWriter)))

(def config
  (-> (config/create {:prefix "slack-backfill"})
      config-cli/add-provider))

(def event->date
  (comp time-util/format-inst-day time-util/ts->inst event/event-ts))

(def json-writer (json/json-writer-fn {}))

(defonce json-writers (atom {}))

(defn json-writer-for [opts channel-id {:strs [team] :as event}]
  (let [target-dir (:archive/path opts)]
    (when team
      (when-let [date (event->date event)]
        (let [path (format "%s/%s/%s/%s.jsonl" target-dir team channel-id date)]
          (or
           (get @json-writers path)
           (let [_ (.mkdirs (io/file target-dir team channel-id))
                 w (json-writer (io/writer path))]
             (when (:verbose opts 0)
               (println date))
             (swap! json-writers assoc path w)
             w)))))))

(defn fetch-logs [opts]
  (let [conn (api/conn (config/get config :slack/bot-token))]
    (doseq [{:channel/keys [name id] :as channel} (api/conversations conn)]
      (println "Fetching" id (str "#" name))
      (let [history (api/history conn {:channel id})]
        (when (seq history)
          (doseq [message history]
            (when (< 1 (:verbose opts 0))
              (prn message))
            (when-let [^JSONWriter w (json-writer-for opts id message)]
              (.writeObject w message)
              (.write (.-w w) "\n")
              (when (get message "thread_ts")
                (doseq [message (api/replies conn {"channel" id "ts" (get message "ts")})]
                  (.writeObject w message)
                  (.write (.-w w) "\n")))
              (.flush w))))))))

(defn backfill
  "Run the backfill process"
  [opts]
  (.mkdirs (io/file (:archive/path opts)))
  (fetch-logs
   opts))

(def cmdspec
  {:name "slack-backfill"
   :doc "Backfill slack history"
   :commands ["start" #'backfill]
   :flags
   ["--bot-token <token>" {:key :slack/bot-token
                           :doc "Slack bot token"}
    "--path <path>"  {:key :archive/path
                      :doc "Location where the write the archive (directory)"
                      :default "archive"}
    "--verbose,-v" "Increase verbosity"]})

(defn -main [& argv]
  (cli/dispatch* cmdspec argv))


(comment
  (def conn (api/conn (config/get config :slack/bot-token)))

  (def channels (api/user-conversations conn))

  (def datomic-history (api/history conn {:channel "C03RZMDSH"}))

  (map    (take 10 datomic-history))

  (backfill {:archive/path "archive"})

  )
