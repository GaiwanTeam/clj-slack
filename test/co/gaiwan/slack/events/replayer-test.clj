(ns co.gaiwan.slack.events.replayer-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [co.gaiwan.slack.events.replayer :as replayer]))

(defn duration [events]
  (- (replayer/get-event-time (last events))
     (replayer/get-event-time (first events))))

(duration @(:sorted-events replayer/replayer))

(deftest speed-test [events]
  (is (= (duration (replayer/speed 2 events))
         (/ (duration events) 2))))

(deftest micros->ts-test
  (testing "The formatting of the ts string."
    (is (= 17 (count (replayer/micros->ts 12345678)))
        "Should be 17 (16 digits + 1 dot.")
    (is (= 10
           (str/index-of (replayer/micros->ts 12345678) \.)
           (str/last-index-of (replayer/micros->ts 12345678) \.))
        "Should contain first and only dot at index 10."))
  (testing "Conversion between formats."
    (is (= 1614852449028400 (replayer/ts->micros (replayer/micros->ts 1614852449028400)))
        "Should be parseable by replayer/ts->micros.")
    (is (= "0000000000.012345" (replayer/micros->ts 12345))
        "Small values should be properly padded.")
    (is (= "0000000402.022400" (replayer/micros->ts 402022400))
        "Medium values should be properly padded.")
    (is (= "1614852449.028400" (replayer/micros->ts 1614852449028400))
        "Values of the exact size should match.")))

