(ns repl-sessions.walkthrough
  (:require [clojure.java.io :as io]
            [co.gaiwan.slack.archive :as archive]
            [co.gaiwan.slack.archive.api-resources :as api-resources]
            [co.gaiwan.slack.archive.partition :as partition]
            [co.gaiwan.slack.enrich :as enrich]
            [co.gaiwan.slack.markdown :as markdown]
            [co.gaiwan.slack.normalize :as normalize]
            [co.gaiwan.slack.normalize.messages :as messages]
            [co.gaiwan.slack.raw-archive :as raw-archive]
            [co.gaiwan.slack.raw-archive :as raw]
            [co.gaiwan.slack.test-data.markdown :as md-test-data]
            [co.gaiwan.slack.ui.components :as components]))

;; This library bundles functionality for dealing with Slack data in various way
;; - Dealing with archive data
;; - Dealing with the API (REST)
;; - Dealing with the socket API (websocket, live events)
;; - Parsing Slack-flavored markdown

;; All pieces are loosely coupled, but share common data types. You should
;; probably start by reading [[repl-sessions.abstractions]], to understand these
;; shared types and interfaces.

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
(archive/raw->archive raw-archive-path archive)

;; Alternative which is slower and might introduce duplicates, but is less
;; memory-hungry
(doseq [events (partition-all 10000 (raw-archive/dir-event-seq raw-archive-path [".txt" ".jsonl"]))]
  (partition/into-archive
   archive
   (filter archive/default-event-filter-predicate)
   events)
  (System/gc))

;; Notice that so far we are leaving these events exactly as we got them from
;; Slack, with string keys, underscores, etc.

(def raw-events (archive/slurp-chan-day-raw archive "C06MAR553" "2021-06-12"))

;; The [[co.gaiwan.slack.normalize]] namespace namespace now allows us to
;; process these and turn them into something more clojure-y. It can create two
;; types of Clojure data structures, a "message-tree" and a "message-seq".

(normalize/message-tree raw-events)

;; A message tree

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

;; The archive can be augmented with data from the Slack API: users, channels,
;; emoji.

(def slack-token (System/getProperty "SLACK_TOKEN")
  #_"xoxb-...")

;; Use the API to fetch users/channels/emoji. This will save them in files
;; inside the archive (users.jsonl, channels.jsonl, emoji.json), and add them to
;; the archive map.
(def archive (archive/fetch-api-resources archive slack-token))

(:users archive)
(:channels archive)
(:emoji archive)

;; You can also load them from disk once they have been saved.

(def archive (archive/load-api-resources archive))

;; Now let's look at the markdown parser

(markdown/markdown->hiccup
 "This does not belong in this channel. Perhaps <#C8NUSGWG6|news-and-articles> <@UE1747L7J>?")
;; =>
("This does not belong in this channel. Perhaps "
 [:span.channel [:i "#" "news-and-articles"]]
 " "
 [:span.username [:em "<" "UE1747L7J" ">"]]
 "?")

;; As you can see this has converted the markdown to hiccup, but it's taken a
;; very generic approach to dealing with user and channel data, since it doesn't
;; have any extra information to be able to render those. However we can
;; inject "handlers" to render those specific things.

(defn user-id-handler [[_ user-id] _]
  [:span.username
   [:a {:href (str "https://someteam.slack.com/team/" user-id)}
    "@" (get-in archive [:users user-id :user/name] user-id)]])

(defn emoji-handler [[_ code] _]
  [:span.emoji (get-in archive [:emoji code]
                       (get @markdown/standard-emoji-map code code))])

(defn channel-handler [[_ channel-id channel-name] handlers]
  [:span.channel [:a {:href (str "https://example.com/" channel-id)}
                  "#" (or channel-name channel-id)]])

(def md-handlers
  {:handlers {:user-id user-id-handler
              :emoji emoji-handler
              :channel-id channel-handler}})

(markdown/markdown->hiccup
 "This does not belong in this channel. Perhaps <#C8NUSGWG6|news-and-articles> <@UE1747L7J>? :kaocha:"
 md-handlers)

;; The rendering logic for any type of markdown element can be overridden this
;; way, so it's usable in many different contexts.

;; Now that we have this user, emoji, and channel information, we can "enrich"
;; the messages in the message-seq, so they are completely ready to be rendered
;; without further context.

(def messages
  (-> raw-events
      (->> (take 1000))
      normalize/message-seq
      (enrich/enrich {:users (:users archive)
                      :handlers md-handlers
                      :org-name "clojurians"})))

(user/portal)

(tap>
 (with-meta
   (components/message {:org-name "clojurians"} (rand-nth messages))
   {:portal.viewer/default
    :portal.viewer/hiccup}))
