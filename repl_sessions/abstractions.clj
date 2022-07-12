(ns repl-sessions.abstractions)

;; clj-slack is like a box of legos. A bunch of separate pieces that you can
;; combine in interesting ways to make new things. All lego blocks have the same
;; size of studs on the top, and similarly shaped holes at the bottom. This is
;; what allows you to combine them. clj-slack has a number of shared
;; abstractions that form the interfaces between sub-libraries.

;; ## Raw Slack Events

;; When someone sends a message, adds an emoji reaction, or pins a message, then
;; slack generates an event. We call these "raw slack events". These can be
;; coming to us from the Slack Socket API, from the REST API, or we could be
;; reading them from a newline-delimited JSON file on disk. Outside of our
;; application they are typically represented as JSON. Inside our code we
;; represent them as Clojure maps. Apart from that we do no further munging on
;; these, so they retain their string keys, and use underscores in identifiers.

;; Attention: we represent these with string keys, to make it explicit that
;; these are events directly as they arrive from Slack. Existing code (e.g. in
;; clojurians-log or DOES) may be using keyword keys, this practice is
;; deprecated.

;; Example of a raw slack event:

{"event_ts" "1623196817.046800",
 "item_user" "U0LCHMJTA",
 "ts" "1623196817.046800",
 "user" "U0BBFDED7",
 "reaction" "rocket",
 "item" {"ts" "1623186339.046400", "type" "message", "channel" "C03RZRRMP"},
 "type" "reaction_added"}

;; These always have a `"type"` and a `"ts"`. The `"ts"` is the timestamp of the
;; event in UNIX timestamp format, so in seconds since 1 January 1970, but with
;; microseconds after the decimal points.

;; The `"type"` can be things like `"message"`, `"reaction_added"`,
;; `"channel_joined"`, and many more. Depending on the `"type"` an event will
;; have additional keys, e.g. `"message"` events have a `"subtype"`.
;; `"reaction_added"` events have a `"reaction"`

;; This `"ts"` also serves as a unique identifier for the event (at least we can
;; assume it is unique for a given organization and channel).
;; See [[repl-sessions.raw-events]] for an exploration of the type.

;; We don't normally add to or update raw event maps, so e.g. we don't assoc
;; additional information into them, or coerce values to other types. We just
;; deal with them in the shape that we get them from Slack.

;; Tip: Use `:strs` to destructure raw events:

(let [{:strs [ts type subtype]} event]
  ,,,)

;; Events are useful for real-time updates. Any component that needs to stay up
;; to date about what is happening can be consuming raw events, and then
;; processing them incrementally.

;; Places where we use raw events

;; - co.gaiwan.slack.socket.api : emits raw events to listeners
;; - co.gaiwan.slack.archive : organize raw events on disk per channel and day
;; - co.gaiwan.slack.time-util : contains code for interpreting `ts` values as timestamps
;; - co.gaiwan.slack.api/history : fetch event history directly from the API

;; ## co.gaiwan.protocols/EventSource

;; A component that wants to receive Slack events should do so by registering
;; itself to an `EventSource`. This is a simple interface with two methods,
;; `add-listener`, and `remove-listener`. It works a lot like `add-watch` on atoms.

;; The idea is that we can have multiple event sources: coming from the socket API,
;; coming from Pusher, coming from disk (for replay).

;; You can also imagine "decorator" event sources, e.g. an event source that wraps
;; another event source, but shifts the timestamps by an offset.

;; What this really represents is a stream of values, and so you could also
;; imagine representing these as e.g. core.async channels. In that case the
;; aforementioned decorator could really be a transducer on the channel. This
;; may in some cases be the more suitable representation, in which case we can
;; easily transform an `EventSource` into a channel, but we didn't want to
;; always impose the core.async dependency, so we chose for a very simple base
;; abstraction, an event listener pattern, that others can be derived from.

;; ## Message tree, message seq, message store

;; ### Message tree

;; Raw events are useful for staying up to date incrementally, but if we need to
;; e.g. render a UI then we need to combine them, to get a complete picture of
;; what's visible in the channel. There could be dozens of events relating to the
;; same message, and we need to combine them all to get a complete picture.

;; - someone posts a message
;; - the message gets edited
;; - someone adds a reaction
;; - someone posts a reply
;; - someone adds another reaction
;; - etc...

;; For this we can maintain a data structure called a message tree. The code that
;; does so is currently in [[co.gaiwan.slack.normalize]]
;; and [[co.gaiwan.slack.normalize.messages]].

;; A message-tree is a nested, normalized map structure. It has `ts` values as its
;; keys, and its values are maps with namespaced keys.

(into (sorted-map)
      [[ts {:message/timestamp ts
            :message/text text
            :message/channel-id channel
            :message/user-id user}]])

;; This combination of using a sorted map, and timestamps as keys, gives us two
;; interesting properties:

;; - we can easily update or retrieve a message given its time stamp
;; - we can easily get all messages ordered by timestamp

;; We can build up this structure, and keep it up-to-date over time, by
;; consuming raw slack events (see [[co.gaiwan.slack.normalize.messages]]).
;; Reactions and replies get added to the top level message they belong to, and
;; edits and deletions are processed as well, so this essentially gives us
;; a "read model", a snapshot view over what the current state is.

;; Replies are nested, by adding another sorted-map under the `:message/replies`
;; key.

{"1623510278.217800"
 {:message/timestamp "1623510278.217800"
  :message/text "The first release of *scittle* is available (v0.0.1)!"
  :message/channel-id "C06MAR553"
  :message/user-id "U04V15CAJ"
  :message/reactions {"tada" 39
                      "cljs" 16
                      "rocket" 18
                      "sci" 8
                      "heart" 6
                      "wizard" 4}
  :message/replies
  {"1623512147.219300"
   {:message/timestamp "1623512147.219300"
    :message/text "This is just… sorcery! :wizard:"
    :message/channel-id "C06MAR553"
    :message/user-id "U04V70XH6"}}}}

;; Before we can actually render these messages we need some more information.
;; We need to look up user names, channel names, and parse markdown, which
;; includes converting links to channels, looking up custom emoji, etc. This is
;; all done in [[co.gaiwan.slack.enrich]].

;; ### Message-seq

;; To actually render these message we need to iterate over them in timestamp
;; order. Luckily for us we were using a sorted map, so this is as simple as taking

(vals message-tree)

;; Except we need to do so recursively for nested (threaded) messages, this is
;; handled in [[co.gaiwan.clj-slack.normalize/mtree->mseq]]

;; ### Message-store

;; In typical usage we'll likely end up having a message-tree in an atom in memory,
;; either on the server side, or on the client side (in the browser), which is kept
;; up to date over time. We'll refer to this as a "message store". There is
;; currently no supporting code yet for working with message stores.

;; You can imagine a variant of this where there's an extra wrapper map to segment
;; messages per-channel, especially on the server where it's more common for us to
;; be dealing with data from multiple channels at once.
