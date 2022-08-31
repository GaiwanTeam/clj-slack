(ns co.gaiwan.slack.normalize-test
  (:require [co.gaiwan.slack.normalize :as normalize]
            [co.gaiwan.slack.test-data.raw-events :as raw-events]
            [clojure.test :refer :all]))

(deftest channel-join+reaction
  (is (= [{:message/timestamp "1593697538.043200"
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
         (normalize/message-seq raw-events/channel-joins+reaction))))

(deftest single-reply-test
  (is (= [{:message/timestamp "1550831541.063800"
           :message/text "thankyou <@U82DUDVMH> you have made my day"
           :message/channel-id "C064BA6G2"
           :message/user-id "U793EL04V"
           :message/reply-timestamps #{"1550832057.067300"}
           :message/replies
           [{:message/timestamp "1550832057.067300"
             :message/text "/hat-tip"
             :message/channel-id "C064BA6G2"
             :message/user-id "U82DUDVMH"
             :message/thread-ts "1550831541.063800"}]}]
         (normalize/message-seq raw-events/single-reply))))

(deftest replies+broadcast
  (is (= [{:message/timestamp "1614822402.022400"
           :message/text
           "Hello all does anyone have any idea where I should be passing the `collection-format : \"csv\"` option in order to parse query parameters values in a comma separated list? Looking through the source code it looks like in `schema-tools.swagger.core` on line 51 the default option is `multi` but I can't seem to find the right spot to overide that default."
           :message/channel-id "C7YF1SBT3"
           :message/user-id "U010ACDMUHX"
           :message/reply-timestamps #{"1614852449.028400" "1614852801.028600" "1614853014.028900"},
           :message/replies
           [{:message/timestamp "1614852449.028400"
             :message/text
             "Hmm in Compojure-api/Ring-swagger this was using describe/field function but it is a bit different here."
             :message/channel-id "C7YF1SBT3"
             :message/user-id "U061V0GG2"
             :message/thread-ts "1614822402.022400"}
            {:message/timestamp "1614852801.028600"
             :message/text
             "Aha yes. `schema-tools.core/schema` allows attaching additional data to a schema.\n\n`(st/schema [s/Str] {:swagger/collection-format \"csv\"})`"
             :message/channel-id "C7YF1SBT3"
             :message/user-id "U061V0GG2"
             :message/thread-ts "1614822402.022400"
             :message/thread-broadcast? true}
            {:message/timestamp "1614853014.028900"
             :message/text
             "Does seem to be documented.\n\nImpl is here: <https://github.com/metosin/schema-tools/blob/master/src/schema_tools/swagger/core.cljc#L160-L164>\n\nKeys with :swagger ns from the additional data are merged to the properties swagger spec."
             :message/channel-id "C7YF1SBT3"
             :message/user-id "U061V0GG2"
             :message/thread-ts "1614822402.022400"}]}
          {:message/timestamp "1614852801.028600"
           :message/text
           "Aha yes. `schema-tools.core/schema` allows attaching additional data to a schema.\n\n`(st/schema [s/Str] {:swagger/collection-format \"csv\"})`"
           :message/channel-id "C7YF1SBT3"
           :message/user-id "U061V0GG2"
           :message/thread-ts "1614822402.022400"
           :message/thread-broadcast? true}]
         (normalize/message-seq raw-events/replies+broadcast))))


(comment
  (require 'kaocha.repl)
  (kaocha.repl/run)
  )
