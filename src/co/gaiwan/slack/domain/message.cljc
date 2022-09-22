(ns co.gaiwan.slack.domain.message
  "Schema and logic for manipulating Message data")

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
   [:message/image? {:optional true} boolean?]])
