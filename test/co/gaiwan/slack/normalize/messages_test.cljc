(ns co.gaiwan.slack.normalize.messages-test
  (:require [co.gaiwan.slack.normalize.messages :as messages]
            [co.gaiwan.slack.test-data.raw-events :as raw-events]
            [clojure.test :refer [deftest is are testing run-tests use-fixtures]]))

(deftest add-message-test
  (testing "type:message / subtype:<blank>"
    (is (= {"1621508880.437600" {:message/timestamp "1621508880.437600",
                                 :message/text "Thanks so, so much <@UB5S3V9F0>!! That was awesome",
                                 :message/channel-id "C015DQFEGMT",
                                 :message/user-id "U01UU0ELRM5"}}
           (messages/add-event {} raw-events/message))))

  (testing "type:message / subtype:bot_message"
    (is (= {"1593099709.291400" {:message/timestamp "1593099709.291400",
                                 :message/text "Andy Sturrock, BP, United Kingdom visited Datadog",
                                 :message/channel-id "G015WHGM9LJ",
                                 :message/user-id "B015WNQ222X"}}
           (messages/add-event {} raw-events/bot-message))))

  (testing "type:message with thread_ts"
    (is (= {"1550831541.063800" {:message/timestamp "1550831541.063800"
                                 :message/text "thankyou <@U82DUDVMH> you have made my day"
                                 :message/channel-id "C064BA6G2"
                                 :message/user-id "U793EL04V"
                                 :message/reply-timestamps #{"1550832057.067300"}}
            "1550832057.067300" {:message/timestamp "1550832057.067300"
                                 :message/text "/hat-tip"
                                 :message/channel-id "C064BA6G2"
                                 :message/user-id "U82DUDVMH"
                                 :message/thread-ts "1550831541.063800"}}
           (reduce messages/add-event (sorted-map) raw-events/single-reply))))

  (testing "type:message / subtype:message_changed"
    (is (= {"1602786078.090100"
            {:message/timestamp "1602786078.090100",
             :message/text
             "<@U01ARPAMX0V> and if you need any specific demo(s), let us know.",
             :message/channel-id "C01CK1556QH",
             :message/user-id "U01BM1H8JP7"}}
           (reduce messages/add-event (sorted-map) raw-events/message-changed))))

  (testing "deletion"
    (= [{}
        {"1652269908.038599"
         {:message/timestamp "1652269908.038599"
          :message/text " set up a reminder on “Get your copy of A Radical Enterprise by @Matt K. Parker he/him (Speaker/Author) at happy hour today, thanks to IT Revolution (xpo-itrevolution)!\nhttps://devopsenterprise.slack.com/files/UATE4LJ94/F02GCSAMUDT/image.png” in this channel at 1:17PM today, Eastern Daylight Time."
          :message/channel-id "C015FGB49UK"
          :message/user-id "UATE4LJ94"
          :message/emphasis? true}}
        {"1652269908.038599"
         {:message/timestamp "1652269908.038599"
          :message/channel-id "C015FGB49UK"
          :message/deleted? true
          :message/text "This message was deleted."
          :message/user-id "USLACKBOT"
          :message/system? true}}]
       (reductions messages/add-event {} raw-events/deletion))))

(deftest affected-keys-test
  (is (= ["1593099709.291400"]
         (messages/affected-keys raw-events/bot-message)))

  (testing "Reactions affect the message they are a reaction to"
    (is (= [["1621543244.008400"]
            ["1621543244.008400"]]
           (map messages/affected-keys raw-events/message+reaction))))

  (testing "replies affect themselves and the parent"
    (is (= [["1614822402.022400"]
            ["1614852449.028400" "1614822402.022400"]
            ["1614852801.028600" "1614822402.022400"]
            ["1614853014.028900" "1614822402.022400"]]
           (map messages/affected-keys raw-events/replies+broadcast))))

  (testing "pin event"
    (is (= [["1621258056.046800"] ["1621258056.046800"]]
           (map messages/affected-keys raw-events/pin-message)))))

(deftest enrich-messages
  (testing "new event of reply message with opts"
  ;; reply to single message and expand both message and reply with info about the users
    (let [users        repl-sessions.affected-messages/users
          user+profile co.gaiwan.slack.normalize.web-api/user+profile
          opts         {:users    (into {}
                                        (map (juxt :user/id identity))
                                        (map user+profile (get users "members")))
                        :org-name "gaiwanteam"}
          message-tree raw-events/msg-tree-test
          event        raw-events/reply-ariel]
      (messages/add-event message-tree event opts)))

  (def message-tree #_(reduce
                       messages/add-event
                       {} (take 5 (remove #(get % "bot_id")
                                          (get repl-sessions.affected-messages/events "messages"))))
    {"1652772696.340349" {:message/timestamp  "1652772696.340349",
                          :message/text       "test",
                          :message/channel-id nil,
                          :message/user-id    "U01FVSUGVN3"},
     "1652772241.205779" {:message/timestamp  "1652772241.205779"
                          :message/text       "ok"
                          :message/channel-id nil
                          :message/user-id    "U01FVSUGVN3"},
     "1652253142.426959" {:message/timestamp  "1652253142.426959",
                          :message/text       "hello",
                          :message/channel-id nil,
                          :message/user-id    "U01G7GP6L5B"},
     "1652176239.817669" {:message/timestamp  "1652176239.817669"
                          :message/text       "1"
                          :message/channel-id nil
                          :message/user-id    "U01FVSUGVN3"},
     "1652176238.716789" {:message/timestamp  "1652176238.716789",
                          :message/text       "testing",
                          :message/channel-id nil,
                          :message/user-id    "U01FVSUGVN3"}})

  (testing "new event of deleting message with opts"
    (let [users        repl-sessions.affected-messages/users
          user+profile co.gaiwan.slack.normalize.web-api/user+profile
          opts         {:users    (into {}
                                        (map (juxt :user/id identity))
                                        (map user+profile (get users "members")))
                        :org-name "gaiwanteam"}
          event        [{"subtype"  "reminder_add",
                         "event_ts" "1652176239.817669",
                         "ts"       "1652176239.817669",
                         "user"     "U01FVSUGVN3",
                         "text"     "1",
                         "type"     "message",
                         "channel"  "C015FGB49UK",
                         "team"     "TASMB716H"}
                        {"subtype"          "message_deleted",
                         "event_ts"         "1652269917.081500",
                         "ts"               "1652269917.081500",
                         "previous_message" {"subtype" "reminder_add",
                                             "ts"      "1652176239.817669",
                                             "user"    "U01FVSUGVN3",
                                             "text"    "1",
                                             "type"    "message"},
                         "type"             "message",
                         "hidden"           true,
                         "deleted_ts"       "1652176239.817669",
                         "channel"          "C015FGB49UK"}]]
      (messages/add-event message-tree event opts))))


(comment

  (defn find-user-by-id [id]
    (get (get {:users    (into {}
                               (map (juxt :user/id identity))
                               (map co.gaiwan.slack.normalize.web-api/user+profile (get repl-sessions.affected-messages/users "members")))
               :org-name "gaiwanteam"} :users) id))
  (find-user-by-id "U01FVSUGVN3")

  ;; remove prefix in the repl to messages in the msg-tree
  (set! *print-namespace-maps* false))