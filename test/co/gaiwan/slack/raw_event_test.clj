(ns co.gaiwan.slack.raw-event-test
  (:require [co.gaiwan.slack.raw-event :as raw-event]
            [clojure.test :refer [deftest testing is are run-tests]]
            [co.gaiwan.slack.test-data.raw-events :as raw-events]))

(deftest message-ts-test
  (is (= ["1593697538.043200"
          "1593700697.043500"
          "1593697538.043200"]
         (map raw-event/message-ts raw-events/channel-joins+reaction)))
  (is (apply = (map raw-event/message-ts raw-events/deletion)))

  (is ["1621258056.046800"
       "1621258056.046800"]
      (map raw-event/message-ts raw-events/pin-message)))

(deftest regular-message?-test
  (is (= [false false false]
         (map raw-event/regular-message? raw-events/channel-joins+reaction))))

(deftest thread-broadcast?-test
  (is (= [false false true false]
         (map raw-event/thread-broadcast? raw-events/replies+broadcast))))
