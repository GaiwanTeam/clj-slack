(ns repl-sessions.blocks
  "An exploration of the \"blocks\" data we get from slack"
  (:require [clojure.java.io :as io]
            [co.gaiwan.slack.raw-archive :as raw]
            [co.gaiwan.slack.archive :as archive]
            [co.gaiwan.slack.normalize.messages :as messages]
            [co.gaiwan.slack.normalize :as normalize]))

(def raw-archive-path "/home/arne/github/clojurians-log/logs"
  #_"/path/to/clojurians-log/logs")

(def raw-events (raw/dir-event-seq raw-archive-path))

;; An example
(first (keep #(get % "blocks") raw-events))
;;=>
[{"block_id" "t8AA",
  "type" "rich_text",
  "elements"
  [{"type" "rich_text_section",
    "elements"
    [{"text"
      "I agree... selecting twice doesn't feel great. It breaks the composability of the navigator abstraction",
      "type" "text"}]}]}]

;; It seems like blocks is a nested structure, it's a seq of maps, each map has
;; a type, and possible an "elements" key with more blocks. Let's see what types
;; there are, and what kind of keys we encounter

(transduce
 cat
 (completing
  (fn handle-blk [acc blk]
    (let [acc (update acc (get blk "type") (fnil into #{}) (keys blk))]
      (if (get blk "elements")
        (reduce handle-blk acc (get blk "elements"))
        acc)))
  identity)
 {}
 (keep #(get % "blocks") raw-events))

{"mrkdwn"                 #{"text" "type" "verbatim"},
 "emoji"                  #{"name" "style" "type" "skin_tone"},
 "rich_text_list"         #{"offset" "border" "style" "indent" "type" "elements"},
 "user"                   #{"user_id" "style" "type"},
 "section"                #{"block_id" "text" "type"},
 "broadcast"              #{"range" "type"},
 "context"                #{"block_id" "type" "elements"},
 "text"                   #{"style" "text" "type"},
 "rich_text_quote"        #{"border" "type" "elements"},
 "link"                   #{"url" "unsafe" "style" "text" "type"},
 "rich_text_section"      #{"type" "elements"},
 "channel"                #{"channel_id" "style" "type"},
 "color"                  #{"value" "type"},
 "rich_text_preformatted" #{"border" "type" "elements"},
 "actions"                #{"block_id" "type" "elements"},
 "button"                 #{"url" "style" "confirm" "text" "type" "action_id"},
 "rich_text"              #{"block_id" "type" "elements"},
 "usergroup"              #{"type" "usergroup_id"}}
