(ns co.gaiwan.slack.domain.message
  "Schema and logic for manipulating Message data"
  (:require [co.gaiwan.slack.time-util :as time-util]))

(def ?Timestamp [:re #"\d{10}\.\d{6}"])

(def ?Message
  [:map
   [:message/timestamp ?Timestamp]
   [:message/channel-id string?]
   [:message/user-id string?]
   [:message/text string?]
   [:message/thread-ts {:optional true} ?Timestamp]
   [:message/thread-broadcast? {:optional true} boolean?]
   [:message/reply-timestamps [:set ?Timestamp]]
   [:message/deleted? {:optional true} boolean?]
   [:message/system? {:optional true} boolean?]
   [:message/image? {:optional true} boolean?]
   [:message/reactions {:optional true} [:map-of string? int?]]])

(defn reaction-count [{:message/keys [reactions]}]
  (reduce + (vals reactions)))

(defn replies-count [{:message/keys [reply-timestamps]}]
  (count reply-timestamps))

(defn relevancy [message {:keys [start-time-ms exponent
                                 reaction-factor replies-factor]
                          :or {exponent 1.2
                               reaction-factor 2
                               replies-factor 1}}]
  (let [reaction-count (reaction-count message)
        replies-count  (replies-count message)
        micros-elapsed (- (time-util/ts->micros (:message/timestamp message))
                          (* 1000 start-time-ms))
        score          (+ (* reaction-count reaction-factor)
                          (* replies-count replies-factor))]
    (/ score (Math/pow (inc micros-elapsed) exponent))))
