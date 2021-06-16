(ns slack-api
  (:require [lambdaisland.glogc.log :as log]
            [co.gaiwan.slack.api.core :as clj-slack]))

;; export environment variable SLACK_TOKEN
(def conn (clj-slack/conn))

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

(count (clj-slack/get-users conn))

(def users (clj-slack/get-users conn))
(def channels (clj-slack/get-channels conn))

(spit "resources/co/gaiwan/slack/demo_users.edn"
      (with-out-str
        (clojure.pprint/pprint
         (take 100 (shuffle users)))))

(spit "resources/co/gaiwan/slack/demo_channels.edn"
      (with-out-str
        (clojure.pprint/pprint
         (take 100 (shuffle channels)))))
