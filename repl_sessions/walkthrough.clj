(ns repl-sessions.walkthrough
  (:require [clojure.java.io :as io]
            [co.gaiwan.slack.raw-archive :as raw]
            [co.gaiwan.slack.archive :as archive]
            [co.gaiwan.slack.normalize.messages :as messages]
            [co.gaiwan.slack.normalize :as normalize]))

;; This library bundles functionality for dealing with Slack data in various way
;; - Dealing with archive data
;; - Dealing with the API (REST)
;; - Dealing with the socket API (websocket, live events)
;; - Parsing Slack-flavored markdown

;; All pieces are loosely coupled, but share common data types.

;; ## Dealing with Archive Data

;; Start by grabbing the clojurians-log data, Arne can give you access

;; git clone https://github.com/plexus/clojurians-log

;; This is what we call a "raw" archive, it's a pile of text files, each one
;; containing JSON objects, one per line. (Also known as json-lines or
;; newline-delimited-JSON files). Most of these were created by the
;; RTMbot (https://github.com/clojureverse/rtmbot), some were created by
;; backfilling through the Slack API.
;;
;; Events might be duplicated, or out of order, or missing. Generally we have a
;; single json-lines file for each day, across all channels.

;; The [[co.gaiwan.slack.raw-archive]] namespace contains code for working with
;; such a raw archive. [[raw/dir-event-seq]] is a highly optimized routine for
;; reading all of the text files, parsing all the JSON, and returning a single
;; sequence (actually an Eduction) with all the raw events.

(def raw-archive-path "/home/arne/github/clojurians-log/logs"
  #_"/path/to/clojurians-log/logs")

(def raw-events (raw/dir-event-seq raw-archive-path))

(first raw-events)
;;=>
{"event_ts" "1623196817.046800",
 "item_user" "U0LCHMJTA",
 "ts" "1623196817.046800",
 "user" "U0BBFDED7",
 "reaction" "rocket",
 "item" {"ts" "1623186339.046400", "type" "message", "channel" "C03RZRRMP"},
 "type" "reaction_added"}

;; Besides this raw archive we have another structure, which we call a (regular)
;; archive. It contains the same type of json-lines files with raw events, but
;; they are split out per date and channel, since that is typically how we
;; access them, and duplicates have been removed. There's also a corresponding
;; data structure in memory where we keep some information about such an
;; archive. At a minimum it contains the directory where the archive is stored
;; on disk, {:archive-dir "..."}, but it can for instance also contain
;; timestamps of events that have been already been processed, so we can
;; de-duplicate them.

;; The [[co.gaiwan.slack.archive]] namespace contains code for creating and
;; working with such an archive. Let's create one based on our raw archive.

(def archive (archive/archive "/tmp/cljians-archive"))

;; This can take a minute or two
(archive/raw->archive raw-archive-path (archive/archive archive-path))

;; Notice that so far we are leaving these events exactly as we got them from
;; Slack, with string keys, underscores, etc.

(def raw-events (archive/slurp-chan-day-raw archive "C06MAR553" "2021-06-12"))

;; The [[co.gaiwan.slack.normalize]] namespace namespace now allows us to
;; process these and turn them into something more clojure-y. It can create two
;; types of Clojure data structures, a "message-map" and a "message-seq".

(normalize/message-map raw-events)

;; A message map

;; - has keys that are message timestamps
;; - It's a sorted map, so `vals` returns messages in timestamp order
;; - values are normalized EDN representations
;; - reactions are added to each message under `:message/reactions`
;; - replies are added under `:message/replies`, themselves a sorted map keyed by timestamp

;; A message-seq is similar, but instead of being a map it's a sequence of EDN
;; values, one for each message, still with reactions and replies, but the
;; replies are no longer a map, they are also a seq.

(normalize/message-seq raw-events)

;; The map format is more convenient for adding more events to as they arrive,
;; the seq representation is more suitable for rendering.
