(ns co.gaiwan.slack.api
  "Request things from the Slack API"
  (:require [lambdaisland.glogc :as log]
            [co.gaiwan.slack.api.middleware :as mw]
            [co.gaiwan.slack.api.web :as web]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Slack API functions

(defn conn
  ([]
   (conn (System/getenv "SLACK_TOKEN")))
  ([slack-token]
   {:api-url "https://slack.com/api" :token slack-token}))

(defn simple-endpoint [endpoint]
  (mw/wrap-result
   (mw/wrap-retry-exception
    5
    (mw/wrap-rate-limit
     (fn self
       ([connection]
        (self connection {}))
       ([connection opt]
        (web/slack-request connection endpoint opt)))))))

(defn collection-endpoint
  [key endpoint]
  (mw/wrap-paginate
   #(log/info :slack-api/pagination-error-resp %)
   key
   (simple-endpoint endpoint)))

(def emoji (collection-endpoint :emoji "emoji.list"))
(def users (collection-endpoint :members "users.list"))

(def conversations (collection-endpoint :channels "conversations.list"))
(def history (collection-endpoint :messages "conversations.history"))
(def replies (collection-endpoint :messages "conversations.replies"))

(def pins (collection-endpoint :items "pins.list"))

(def conversations-join (simple-endpoint "conversations.join"))

(defn join-channel [conn name]
  (let [channel-id (->> (conversations conn)
                        (filter (comp #{name} :name_normalized))
                        first
                        :id)]
    (conversations-join conn {:channel channel-id})))
