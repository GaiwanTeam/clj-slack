(ns slack-api
  (:require [io.pedestal.log :as log]
            [co.gaiwan.clj-slack :as slack-api]))

(def conn
  (slack-api/slack-conn "https://slack.com/api" "xoxb-1111111111111-222222222222-333333333333333333"))

(slack-api/get-emoji conn)
(slack-api/get-channels conn)

(def channel-id "A12345678LOL")
(slack-api/get-history conn {:channel channel-id})
(slack-api/get-pins conn {:channel channel-id})

(let [users (slack-api/get-users conn)]
  (if (slack-api/error? users)
    ((log/error :slack-api/get-users-failed {:resp users})
     (throw (ex-info "Fetching users failed" {:resp users}))))
  users)
