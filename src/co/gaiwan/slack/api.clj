(ns co.gaiwan.slack.api
  "Request things from the Slack API"
  (:require [lambdaisland.glogc :as log]
            [co.gaiwan.slack.api.middleware :as mw]
            [co.gaiwan.slack.api.web :as web]
            [co.gaiwan.slack.normalize.web-api :as normalize-web-api]))

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

(def emoji* (simple-endpoint "emoji.list"))
(def emoji (mw/wrap-get emoji "emoji"))

(def users* (collection-endpoint :members "users.list"))
(def users (mw/wrap-coerce users* normalize-web-api/user+profile))

(def conversations* (collection-endpoint :channels "conversations.list"))
(def conversations (mw/wrap-coerce conversations* normalize-web-api/channel))

(def history
  "(get-history conn {:channel channel-id})"
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

(defn error?
  "Is the response an error?

  This checks for a couple of different cases that might arise. Generally it's
  advisable to always check if a response is an error before trying to use its
  results.

  Slack returns an :ok true/false key in every response. For paginated
  collection responses we normally unwrap the outer map, unless (= :ok false),
  so this should also work on collection responses.

  For rare exceptions where Slack returns a non-200 response with an empty body
  we simply return `:error`."
  [response]
  (or (= :error response)
      (and (map? response) (false? (:ok response)))))
