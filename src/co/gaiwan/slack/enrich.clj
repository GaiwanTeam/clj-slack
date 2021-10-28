(ns co.gaiwan.slack.enrich
  "Take message data, combine it with user and channel info, and parse the markdown."
  (:require [clojure.walk :as walk]
            [co.gaiwan.slack.time-util :as time-util]
            [co.gaiwan.slack.markdown :as markdown]))

(defn enrich
  ([message-tree]
   (enrich message-tree nil))
  ([message-tree {:keys [users handlers]}]
   (walk/prewalk
    (fn [o]
      (if-not (map? o)
        o
        (try
          (cond-> o
            (and users (contains? o :message/user-id))
            (assoc :message/user (get users (:message/user-id o)))
            (contains? o :message/text)
            (assoc :message/hiccup (markdown/markdown->hiccup (:message/text o) {:handlers handlers}))
            (contains? o :message/timestamp)
            (assoc :message/inst (time-util/ts->inst (:message/timestamp o))))
          (catch Exception e
            (prn o)
            (throw e)))))
    message-tree)))
