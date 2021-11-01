(ns co.gaiwan.slack.normalize
  (:require [co.gaiwan.slack.normalize.messages :as messages]))

(defn message-data
  "Given a seqable of raw events, normalize them to proper EDN event
  representations. Thread replies and reactions are added to the message they
  refer to, so the result potentially has a single level of nesting."
  [raw-events]
  (messages/message-data raw-events))
