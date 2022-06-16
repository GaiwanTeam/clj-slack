(ns co.gaiwan.slack.enrich
  "Take message data, combine it with user and channel info, and parse the markdown."
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [co.gaiwan.slack.time-util :as time-util]
            [co.gaiwan.slack.markdown :as markdown]))

(defn channel-link [org-name channel-id]
  (str "https://" org-name ".slack.com/archives/" channel-id))

(defn user-profile-link [org-name user-id]
  (str "https://" org-name ".slack.com/team/" user-id))

(defn actual-display-name [{:message/keys [user-id]}
                           {:user/keys [name real-name]
                            :user-profile/keys [display-name]}]
  (some #(when-not (str/blank? %) %)
        [display-name real-name name user-id]))

(defn permalink [org-name channel-id ts]
  (if (nil? ts)
    ""
    (let [path (str "/p" (str/replace-first ts "." ""))
          permalink (str (channel-link org-name channel-id) path)]
      permalink)))

(defn enrich
  ([message-tree]
   (enrich message-tree nil))
  ([message-tree {:keys [users handlers user-keys org-name]
                  :or   {user-keys [:user-profile/image-48
                                    :user/name
                                    :user/real-name
                                    :user-profile/display-name]}}]
   (walk/prewalk
    (fn [o]
      (if-not (map? o)
        o
        (let [user-id    (:message/user-id o)
              user       (get users user-id)
              channel-id (:message/channel-id o)
              timestamp  (:message/timestamp o)
              inst       (when (contains? o :message/timestamp)
                           (time-util/ts->inst timestamp))]
          (try
            (cond-> o
              user
              (merge (select-keys user user-keys))
              (contains? o :message/text)
              (assoc :message/hiccup (markdown/markdown->hiccup (:message/text o) {:handlers handlers}))
              inst
              (assoc :message/time (time-util/format-inst-time inst))
              :->
              (-> (dissoc :message/text)
                  (assoc :message/permalink (permalink org-name channel-id timestamp)
                         :channel/link (channel-link org-name channel-id)
                         :user-profile/link (user-profile-link org-name user-id)
                         :user-profile/display-name (actual-display-name o user))))
            (catch Exception e
              (prn o)
              (throw e))))))
    message-tree)))
