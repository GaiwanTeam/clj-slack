(ns slack-api
  (:require [co.gaiwan.clj-slack :as slack-api]))

(def conn
  (slack-api/slack-conn "https://slack.com/api" "xoxb-1111111111111-222222222222-333333333333333333"))

(def channel-id "A12345678LOL")
(slack-api/get-emoji conn)
(slack-api/get-users conn)
(slack-api/get-channels conn)
(slack-api/get-history conn {:channel channel-id})
(slack-api/get-pins conn {:channel channel-id})
