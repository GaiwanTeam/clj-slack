(ns co.gaiwan.slack.api
  "Request things from the Slack API"
  (:require
   [lambdaisland.glogc :as log]
   [charred.api :as charred]
   [co.gaiwan.slack.api.middleware :as mw]
   [co.gaiwan.slack.api.web :as web]
   [co.gaiwan.slack.domain.user :as domain-user]
   [co.gaiwan.slack.domain.channel :as domain-channel]
   [hato.client :as hato]))

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

(defn post-endpoint [endpoint]
  (fn [connection body]
    (let [req {:oauth-token (:token connection)
               :throw-exceptions? true
               :body (charred/write-json-str body)
               :content-type :json
               :as :json}]
      (log/info :request/starting {:url (str (:api-url connection) "/" endpoint) :req (dissoc req :body) :body body})
      (let [resp
            (hato/post (str (:api-url connection) "/" endpoint) req)]
        (log/info :request/done (dissoc resp :request))
        resp))))

(defn collection-endpoint
  [key endpoint]
  (mw/wrap-paginate
   #(log/info :slack-api/pagination-error-resp %)
   key
   (simple-endpoint endpoint)))

(def emoji* (simple-endpoint "emoji.list"))
(def emoji (mw/wrap-get emoji* "emoji"))

(def users* (collection-endpoint :members "users.list"))
(def users (mw/wrap-coerce users* domain-user/raw->user))

(def conversations* (collection-endpoint :channels "conversations.list"))
(def conversations (mw/wrap-coerce conversations* domain-channel/raw->channel))

(def history
  "(history conn {:channel channel-id})"
  (collection-endpoint :messages "conversations.history"))

(def replies (collection-endpoint :messages "conversations.replies"))

(def pins (collection-endpoint :items "pins.list"))

(def conversations-join (simple-endpoint "conversations.join"))

(def chat-post-message (simple-endpoint "chat.postMessage"))

(defn send-message
  "Post a message to a specific channel"
  [conn channel-id msg]
  (chat-post-message conn {:channel channel-id
                           :text msg}))

(defn join-channel
  "Have the bot join a given channel"
  [conn channel-id]
  (conversations-join conn {:channel channel-id}))

(def error? web/error?)
