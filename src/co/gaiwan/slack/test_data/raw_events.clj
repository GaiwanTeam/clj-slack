(ns co.gaiwan.slack.test-data.raw-events
  "Sample sequences of raw events

  Used for unit tests and UI (visual) testing.")

(def message
  "A plain message event without subtype"
  {"event_ts" "1621508880.437600"
   "ts" "1621508880.437600"
   "user" "U01UU0ELRM5"
   "client_msg_id" "54bb6fbb-1528-4381-a218-3796139a3144"
   "blocks"
   [{"block_id" "v9+e"
     "type" "rich_text"
     "elements"
     [{"type" "rich_text_section"
       "elements"
       [{"text" "Thanks so, so much " "type" "text"}
        {"user_id" "UB5S3V9F0" "type" "user"}
        {"text" "!! That was awesome" "type" "text"}]}]}]
   "text" "Thanks so, so much <@UB5S3V9F0>!! That was awesome"
   "suppress_notification" false
   "source_team" "TASMB716H"
   "type" "message"
   "channel" "C015DQFEGMT"
   "team" "TASMB716H"
   "user_team" "TASMB716H"})

(def bot-message
  "A message sent by a bot, it has subtype=bot_message. This one is sent by Zapier."
  {"subtype" "bot_message"
   "event_ts" "1593099709.291400"
   "ts" "1593099709.291400"
   "username" "BoothBot"
   "icons" {"emoji" ":space_invader:"
            "image_64"
            "https://a.slack-edge.com/80588/img/emoji_2017_12_06/apple/1f47e.png"}
   "text" "Andy Sturrock, BP, United Kingdom visited Datadog"
   "bot_id" "B015WNQ222X"
   "suppress_notification" false
   "source_team" "TASMB716H"
   "bot_profile" {"team_id" "TASMB716H"
                  "id" "B015WNQ222X"
                  "icons" {"image_48" "https://slack-files2.s3-us-west-2.amazonaws.com/avatars/2017-06-20/200850512066_2d5e268a3b71c87f969c_48.png"
                           "image_72" "https://slack-files2.s3-us-west-2.amazonaws.com/avatars/2017-06-20/200850512066_2d5e268a3b71c87f969c_72.png"
                           "image_36" "https://slack-files2.s3-us-west-2.amazonaws.com/avatars/2017-06-20/200850512066_2d5e268a3b71c87f969c_36.png"}
                  "name" "Zapier"
                  "deleted" false
                  "updated" 1592919641
                  "app_id" "A024R9PQM"}
   "type" "message"
   "channel" "G015WHGM9LJ"
   "team" "TASMB716H"
   "user_team" "TASMB716H"})

(def message-changed
  "Sequence of two events, the first is a message event, the second is a
  message_changed event which alters the original message."
  [{"event_ts" "1602786078.090100"
    "ts" "1602786078.090100"
    "user" "U01BM1H8JP7"
    "client_msg_id" "65916014-b5e3-4551-8ad4-fdb84a4e4a9a"
    "blocks" [{"block_id" "CVuv"
               "type" "rich_text"
               "elements"
               [{"type" "rich_text_section"
                 "elements"
                 [{"user_id" "U01ARPAMX0V" "type" "user"}
                  {"text" " and if you need any specific demo let us know."
                   "type" "text"}]}]}]
    "text" "<@U01ARPAMX0V> and if you need any specific demo, let us know."
    "suppress_notification" false
    "source_team" "TASMB716H"
    "type" "message"
    "channel" "C01CK1556QH"
    "team" "TASMB716H"
    "user_team" "TASMB716H"}

   {"message" {"ts" "1602786078.090100"
               "user" "U01BM1H8JP7"
               "client_msg_id" "65916014-b5e3-4551-8ad4-fdb84a4e4a9a"
               "blocks"
               [{"block_id" "C0uQ"
                 "type" "rich_text"
                 "elements"
                 [{"type" "rich_text_section"
                   "elements"
                   [{"user_id" "U01ARPAMX0V" "type" "user"}
                    {"text" " and if you need any specific demo(s), let us know."
                     "type" "text"}]}]}]
               "text" "<@U01ARPAMX0V> and if you need any specific demo(s), let us know."
               "source_team" "TASMB716H"
               "type" "message"
               "team" "TASMB716H"
               "user_team" "TASMB716H"
               "edited" {"ts" "1602786094.000000", "user" "U01BM1H8JP7"}}
    "subtype" "message_changed"
    "event_ts" "1602786094.090200"
    "ts" "1602786094.090200"
    "previous_message" {"ts" "1602786078.090100"
                        "user" "U01BM1H8JP7"
                        "client_msg_id" "65916014-b5e3-4551-8ad4-fdb84a4e4a9a"
                        "blocks"
                        [{"block_id" "CVuv"
                          "type" "rich_text"
                          "elements"
                          [{"type" "rich_text_section"
                            "elements"
                            [{"user_id" "U01ARPAMX0V" "type" "user"}
                             {"text" " and if you need any specific demo, let us know."
                              "type" "text"}]}]}]
                        "text" "<@U01ARPAMX0V> and if you need any specific demo, let us know."
                        "type" "message"
                        "team" "TASMB716H"}
    "type" "message"
    "hidden" true
    "channel" "C01CK1556QH"}])

(def channel-joins+reaction
  "Two channel join events and a :wave: reaction on one of them."
  [{"channel" "C014LA21AS3" "inviter" "USL9T3Q3X" "subtype" "channel_join"
    "text" "<@U0160GY32VD> has joined the channel"
    "ts" "1593697538.043200" "type" "message" "user" "U0160GY32VD"}
   {"channel" "C014LA21AS3" "inviter" "USL9T3Q3X" "subtype" "channel_join"
    "text" "<@U0168MN6HPY> has joined the channel"
    "ts" "1593700697.043500" "type" "message" "user" "U0168MN6HPY"}
   {"event_ts" "1602509309.001500"
    "item" {"ts" "1593697538.043200" "type" "message" "channel" "C014LA21AS3"}
    "reaction" "wave"
    "ts" "1602509309.001500"
    "type" "reaction_added"
    "user" "U01C0FEJXAR"}])

(def single-reply
  "A message and a (threaded) reply"
  [{"event_ts" "1550831541.063800"
    "ts" "1550831541.063800"
    "user" "U793EL04V"
    "client_msg_id" "73aca9c6-6c33-4de3-a5a0-86847f1c224e"
    "text" "thankyou <@U82DUDVMH> you have made my day"
    "type" "message"
    "channel" "C064BA6G2"
    "team" "T03RZGPFR"}
   {"event_ts" "1550832057.067300"
    "ts" "1550832057.067300"
    "user" "U82DUDVMH"
    "client_msg_id" "79f99599-b72f-4b25-a6ba-2c7686f3600c"
    "text" "/hat-tip"
    "thread_ts" "1550831541.063800"
    "type" "message"
    "channel" "C064BA6G2"
    "team" "T03RZGPFR"}])

(def replies+broadcast
  [{"event_ts" "1614822402.022400"
    "ts" "1614822402.022400"
    "user" "U010ACDMUHX"
    "client_msg_id" "87a75031-48d8-4e3e-b392-5164b77d6381"
    "text"
    "Hello all does anyone have any idea where I should be passing the `collection-format : \"csv\"` option in order to parse query parameters values in a comma separated list? Looking through the source code it looks like in `schema-tools.swagger.core` on line 51 the default option is `multi` but I can't seem to find the right spot to overide that default."
    "suppress_notification" false
    "source_team" "T03RZGPFR"
    "type" "message"
    "channel" "C7YF1SBT3"
    "team" "T03RZGPFR"
    "user_team" "T03RZGPFR"}
   {"event_ts" "1614852449.028400"
    "ts" "1614852449.028400"
    "user" "U061V0GG2"
    "client_msg_id" "beacf6fd-b6f0-4ebf-b313-664b16f0f576"
    "text" "Hmm in Compojure-api/Ring-swagger this was using describe/field function but it is a bit different here."
    "suppress_notification" false
    "thread_ts" "1614822402.022400"
    "source_team" "T03RZGPFR"
    "type" "message"
    "channel" "C7YF1SBT3"
    "team" "T03RZGPFR"
    "user_team" "T03RZGPFR"}
   {"subtype" "thread_broadcast"
    "event_ts" "1614852801.028600"
    "ts" "1614852801.028600"
    "user" "U061V0GG2"
    "client_msg_id" "d6396588-1551-4d21-8a85-69ce28050b21"
    "text"
    "Aha yes. `schema-tools.core/schema` allows attaching additional data to a schema.\n\n`(st/schema [s/Str] {:swagger/collection-format \"csv\"})`"
    "thread_ts" "1614822402.022400"
    "source_team" "T03RZGPFR"
    "type" "message"
    "channel" "C7YF1SBT3"
    "user_team" "T03RZGPFR"}
   {"event_ts" "1614853014.028900"
    "ts" "1614853014.028900"
    "user" "U061V0GG2"
    "client_msg_id" "f47ef1dc-57e5-40b7-b09c-d953f4ad4722"
    "text"
    "Does seem to be documented.\n\nImpl is here: <https://github.com/metosin/schema-tools/blob/master/src/schema_tools/swagger/core.cljc#L160-L164>\n\nKeys with :swagger ns from the additional data are merged to the properties swagger spec."
    "suppress_notification" false
    "thread_ts" "1614822402.022400"
    "source_team" "T03RZGPFR"
    "type" "message"
    "channel" "C7YF1SBT3"
    "team" "T03RZGPFR"
    "user_team" "T03RZGPFR"}])

(def pacific-midnight-2020-10-15 ;;=> 1602745200
  (.toEpochSecond
   (java.time.ZonedDateTime/parse "2020-10-15T00:00:00-07:00[America/Los_Angeles]")))

(def multiple-days-pacific
  "Messages that span the boundary of two dates (two before, two after
  midnight), **in LA time (`America/Los_Angeles`)**"
  [{"ts" "1602745198.022400"
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
    "team" "T03RZGPFR"}
   {"ts" "1602745200.022400"
    "user" "U010ACDMUHX"
    "text" "New day"
    "type" "message"
    "channel" "C7YF1SBT3"
    "team" "T03RZGPFR"}
   {"ts" "1602745201.022400"
    "user" "U010ACDMUHX"
    "text" "New day - part 2"
    "type" "message"
    "channel" "C7YF1SBT3"
    "team" "T03RZGPFR"}])

(def deletion
  "A message that gets posted and then deleted"
  [{"subtype" "reminder_add",
    "event_ts" "1652269908.038599",
    "ts" "1652269908.038599",
    "user" "UATE4LJ94",
    "text"
    " set up a reminder on “Get your copy of A Radical Enterprise by @Matt K. Parker he/him (Speaker/Author) at happy hour today, thanks to IT Revolution (xpo-itrevolution)!\nhttps://devopsenterprise.slack.com/files/UATE4LJ94/F02GCSAMUDT/image.png” in this channel at 1:17PM today, Eastern Daylight Time.",
    "type" "message",
    "channel" "C015FGB49UK",
    "team" "TASMB716H"}

   {"subtype" "message_deleted",
    "event_ts" "1652269917.081500",
    "ts" "1652269917.081500",
    "previous_message" {"subtype" "reminder_add",
                        "ts" "1652269908.038599",
                        "user" "UATE4LJ94",
                        "text"
                        " set up a reminder on “Get your copy of A Radical Enterprise by @Matt K. Parker he/him (Speaker/Author) at happy hour today, thanks to IT Revolution (xpo-itrevolution)!\nhttps://devopsenterprise.slack.com/files/UATE4LJ94/F02GCSAMUDT/image.png” in this channel at 1:17PM today, Eastern Daylight Time.",
                        "type" "message"},
    "type" "message",
    "hidden" true,
    "deleted_ts" "1652269908.038599",
    "channel" "C015FGB49UK"}])
