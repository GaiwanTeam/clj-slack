(ns co.gaiwan.slack.time-util
  "Time conversion and utility functions

  Many of these deal with Slack timestamps, which are high-precision UNIX
  timestamp, which also serve as a primary identifier for Slack events/messages.

  Our current understanding is that these are **unique within a given slack
  channel**. When treating them as identifiers we leave them as strings, but we
  also need them to determine the time and day a given event/message was sent.
  In that case we typically first convert to

  `java.time.Instant` (see [[ts->inst]]), then use java-time formatters to
  format to human readable strings."
  (:require [java-time :as jt]
            [java-time.local :as jt.l]
            [clojure.string :as str])
  (:import [java.time Instant LocalDate]
           [java.time.format DateTimeFormatter]))

(defn ts->inst
  "Convert a Slack timestamp like \"1433399521.000490\" into a java.time.Instant like
  #inst \"2015-06-04T06:32:01.000490Z\""
  [ts]
  (let [[seconds micros] (map #(Double/parseDouble %)
                              (str/split ts #"\."))
        inst (Instant/ofEpochSecond seconds (* 1e3 micros))]
    inst))

(def UTC (jt/zone-id "UTC"))
(def inst-id-formatter (jt/with-zone (jt/formatter "'inst-'yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'") UTC))
(def inst-iso-formatter (jt/with-zone (jt/formatter "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'") UTC))
(def inst-time-formatter (jt/with-zone (jt/formatter "HH:MM:ss") UTC))
(def inst-day-formatter (jt/with-zone (jt/formatter "yyyy-MM-dd") UTC))
(def inst-day-compact-formatter (jt/with-zone (jt/formatter "yyyyMMdd") UTC))

(defn format-inst-id
  "Format an Instant into an id used to link to individual messages. Fun fact: the
  old version of the site did this wrong. It seems it offset all times by three
  hours, so a message posted at 3pm would show up as posted as 6pm. Don't change
  this as it will break existing links."
  [inst]
  (jt/format inst-id-formatter
             (jt/plus inst (jt/hours 3))))

(defn inst-id->inst
  [inst-id-str]
  (as-> inst-id-str $
    (jt/instant inst-id-formatter $)
    (jt/minus $ (jt/hours 3))))

(defn format-inst-time
  "Format an Instant to a simple hour:minute:second time to be displayed in the
  view."
  [inst]
  (when inst
    (jt/format inst-time-formatter inst)))

(defn format-inst-day
  "Format an instant as year-month-day, e.g. 2017-11-20."
  [inst]
  (when inst
    (jt/format inst-day-formatter inst)))

(defn format-inst-day-compact
  "Format an instant as yearr+month+day, e.g. 20171120."
  [inst]
  (when inst
    (jt/format inst-day-compact-formatter inst)))

(defn format-iso
  "Format an instant as yearr+month+day, e.g. 20171120."
  [inst]
  (when inst
    (jt/format inst-iso-formatter inst)))
