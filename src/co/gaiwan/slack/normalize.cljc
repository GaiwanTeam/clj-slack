(ns co.gaiwan.slack.normalize
  "Convert raw messages into normalized and combined data structures, referred to
  as [[message-map]] and [[message-seq]]."
  (:require [co.gaiwan.slack.normalize.messages :as messages]))

(defn message-map
  "Convert raw slack events into a \"message map\".

   - keys are message timestamps
   - It's a sorted map, so `vals` returns messages in order
   - values are normalized EDN representations
   - reactions are added to each message under `:message/reactions`
   - replies are added under `:message/replies`, themselves a sorted map keyed by timestamp

  Use [[messages/add-event]] to further add events to the map."
  [raw-events]
  (reduce
   messages/add-event
   (sorted-map)
   (sort-by #(get % "ts") raw-events)))

(defn mmap->mseq
  "Convert a 'message-map' to a 'message-seq'. In map representation you can look
  up events by timestamp, and replies are also keyed by timestamps. In seq
  representation both the top-level data structure and the `:message/replies`
  are seqs."
  [message-map]
  (map
   (fn [e]
     (if (:message/replies e)
       (update e :message/replies vals)
       e))
   (vals message-map)))

(defn message-seq
  "Given a seqable of raw events, normalize them to proper EDN event
  representations. Thread replies and reactions are added to the message they
  refer to, so the result potentially has a single level of nesting."
  [raw-events]
  (filter
   :message/user-id
   (mmap->mseq (message-map raw-events))))
