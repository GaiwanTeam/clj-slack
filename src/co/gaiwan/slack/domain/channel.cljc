(ns co.gaiwan.slack.domain.channel
  "Schema and logic for manipulating Channel data"
  (:require [co.gaiwan.slack.domain.message :refer [?Message]]))

(def ?Channel
  [:map
   [:channel/id string?]
   [:channel/name string?]
   [:channel/created int?]
   [:channel/creator-id string?]
   [:channel/message-tree {:optional true} [:map-of string? ?Message]]])

(defn raw->channel [{:strs [id name created creator]}]
  {:channel/id id
   :channel/name name
   :channel/created created
   :channel/creator-id creator})
