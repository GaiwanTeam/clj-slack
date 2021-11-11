(ns co.gaiwan.slack.archive.partition-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [co.gaiwan.json-lines :as json-lines]
            [co.gaiwan.slack.archive :as archive]
            [co.gaiwan.slack.archive.partition :as partition]
            [co.gaiwan.slack.test-data.raw-events :as raw-events]
            [co.gaiwan.slack.test-util :as test-util]
            [co.gaiwan.slack.time-util :as time-util])
  (:import (java.time ZoneId)))

(set! *warn-on-reflection* true)

(deftest event-partition-day-str-test
  (is (= "2020-10-15"
         (partition/event-partition-day-str  {"ts" "1602745199.022400"}))))

(deftest timezone-date-boundary-test
  (testing "event timestamps are interpreted as UTC, but indicating time/date
  values in the archive's time zone"
    (let [archive (archive/archive (test-util/temp-dir))]
      (partition/into-archive
       archive
       identity
       raw-events/multiple-days-pacific
       {:event->day (partition/event->day-tz "America/Los_Angeles")})

      (is (= [{"ts" "1602745198.022400"
               "user" "U010ACDMUHX"
               "text" "First message"
               "type" "message"
               "channel" "C7YF1SBT3"
               "team" "T03RZGPFR"}
              {"ts" "1602745199.022400"
               "user" "U010ACDMUHX"
               "text" "Second message"
               "type" "message"
               "channel" "C7YF1SBT3"
               "team" "T03RZGPFR"}]
             (json-lines/slurp-jsonl (str (:dir archive) "/C7YF1SBT3/2020-10-14.jsonl"))))

      (is (= [{"ts" "1602745200.022400",
               "user" "U010ACDMUHX",
               "text" "New day",
               "type" "message",
               "channel" "C7YF1SBT3",
               "team" "T03RZGPFR"}
              {"ts" "1602745201.022400",
               "user" "U010ACDMUHX",
               "text" "New day - part 2",
               "type" "message",
               "channel" "C7YF1SBT3",
               "team" "T03RZGPFR"}]
             (json-lines/slurp-jsonl (str (:dir archive) "/C7YF1SBT3/2020-10-15.jsonl")))))))
