(ns co.gaiwan.slack.test-data.raw-events
  "Sample sequences of raw events

  Used for unit tests and UI (visual) testing.")

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
    "reaction" "wave" "ts" "1602509309.001500" "type" "reaction_added" "user" "U01C0FEJXAR"}])

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
