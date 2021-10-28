(ns co.gaiwan.slack.normalize.events-api-test
  (:require [co.gaiwan.slack.normalize.events-api :as api]
            [clojure.test :refer :all]))

(deftest channel-join+reaction
  (= [{:message/timestamp "1593697538.043200"
       :message/text "<@U0160GY32VD> has joined the channel"
       :message/channel-id "C014LA21AS3"
       :message/user-id "U0160GY32VD"
       :message/system? true
       :message/reactions {"wave" 1}}
      {:message/timestamp "1593700697.043500"
       :message/text "<@U0168MN6HPY> has joined the channel"
       :message/channel-id "C014LA21AS3"
       :message/user-id "U0168MN6HPY"
       :message/system? true}]
     (api/message-data
      [{"channel" "C014LA21AS3", "inviter" "USL9T3Q3X", "subtype" "channel_join",
        "text" "<@U0160GY32VD> has joined the channel",
        "ts" "1593697538.043200", "type" "message", "user" "U0160GY32VD"}
       {"channel" "C014LA21AS3", "inviter" "USL9T3Q3X", "subtype" "channel_join",
        "text" "<@U0168MN6HPY> has joined the channel",
        "ts" "1593700697.043500", "type" "message", "user" "U0168MN6HPY"}
       {"event_ts" "1602509309.001500",
        "item" {"ts" "1593697538.043200", "type" "message", "channel" "C014LA21AS3"},
        "reaction" "wave", "ts" "1602509309.001500", "type" "reaction_added", "user" "U01C0FEJXAR"}])))
