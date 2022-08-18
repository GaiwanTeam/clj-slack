(ns co.gaiwan.slack.time-util
  "Time conversion and utility functions.

  Many of these deal with Slack timestamps: high-precision UNIX
  timestamps which also serve as the primary identifier for Slack
  events/messages.

  Our current understanding is that these are **unique within a given
  Slack channel**. When treating them as identifiers we leave them as
  strings, but we also need them to determine the time and day a given
  event/message was sent. In that case we typically first convert to
  `java.time.Instant` (see [[ts->inst]]), then use java-time
  formatters to format to human readable strings."
  (:require [java-time :as jt]
            [java-time.local :as jt.l]
            [clojure.string :as str])
  (:import (java.time Instant LocalDate ZonedDateTime ZoneId)
           (java.time.format DateTimeFormatter)))

(def ^ZoneId UTC (ZoneId/of "UTC"))
(def ^DateTimeFormatter inst-id-formatter
  (jt/with-zone (jt/formatter "'inst-'yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'") UTC))
(def ^DateTimeFormatter inst-iso-formatter
  (jt/with-zone (jt/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'") UTC))
(def ^DateTimeFormatter inst-time-formatter
  (jt/with-zone (jt/formatter "HH:MM:ss") UTC))
(def ^DateTimeFormatter inst-day-formatter
  (jt/with-zone (jt/formatter "yyyy-MM-dd") UTC))
(def ^DateTimeFormatter inst-day-compact-formatter
  (jt/with-zone (jt/formatter "yyyyMMdd") UTC))

(def ^DateTimeFormatter debug-formatter
  (jt/with-zone (jt/formatter "dd-MM HH:mm:ss.SSSSSS") UTC))

(defn ts->inst
  "Converts a Slack timestamp into a java.time.Instant.

  Example: from \"1433399521.000490\" to
  #inst \"2015-06-04T06:32:01.000490Z\"."
  ^Instant [ts]
  (let [[seconds micros] (map #(Double/parseDouble %)
                              (str/split ts #"\."))
        inst (Instant/ofEpochSecond seconds (* 1e3 micros))]
    inst))

(defn ts->zoned-date-time
  "Converts a Slack timestamp into a ZonedDateTime.

  Returns a ZonedDateTime in the given time zone, assuming the Slack
  timestamp denotes UTC time.

  Example of Slack timestamp: \"1433399521.000490\"."
  ^ZonedDateTime [ts ^ZoneId zone-id]
  (.withZoneSameInstant
   (ZonedDateTime/ofInstant (ts->inst ts) UTC)
   zone-id))

(defn format-inst-id
  "Formats an Instant into an id used to link to individual messages."
  [inst]
  (jt/format inst-id-formatter inst))

(defn inst-id->inst
  [inst-id-str]
  (jt/instant inst-id-formatter inst-id-str))

(defn format-inst-time
  "Formats an Instant to a simple hour:minute:second time.

  This is used to be displayed in the view."
  [inst]
  (when inst
    (jt/format inst-time-formatter inst)))

(defn format-inst-day
  "Formats an Instant as year-month-day, e.g. 2017-11-20."
  [inst]
  (when inst
    (jt/format inst-day-formatter inst)))

(defn format-inst-day-compact
  "Formats an instant as a compact year+month+day, e.g. 20171120."
  [inst]
  (when inst
    (jt/format inst-day-compact-formatter inst)))

(defn format-iso
  "Formats an Instant as an ISO year+month+day, e.g. 20171120T."
  [inst]
  (when inst
    (jt/format inst-iso-formatter inst)))

(defn format-debug
  "non-iso compliant human-readable format"
  [inst]
  (when inst
    (jt/format debug-formatter inst)))
