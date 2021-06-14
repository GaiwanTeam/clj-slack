(ns slack-api
  (:require [io.pedestal.log :as log]
            [co.gaiwan.slack.api.core :as clj-slack]))

(def conn
  (clj-slack/slack-conn "https://slack.com/api" "xoxb-1111111111111-222222222222-333333333333333333"))

(clj-slack/get-emoji conn)
(clj-slack/get-channels conn)

(def channel-id "A12345678LOL")
(clj-slack/get-history conn {:channel channel-id})
(clj-slack/get-pins conn {:channel channel-id})

(let [users (clj-slack/get-users conn)]
  (if (clj-slack/error? users)
    ((log/error :clj-slack/get-users-failed {:resp users})
     (throw (ex-info "Fetching users failed" {:resp users}))))
  users)
