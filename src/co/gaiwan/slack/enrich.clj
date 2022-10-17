(ns co.gaiwan.slack.enrich
  "Take message data, combine it with user and channel info, and parse the markdown."
  (:require [clojure.walk :as walk]
            [clojure.string :as str]
            [co.gaiwan.slack.time-util :as time-util]
            [co.gaiwan.slack.markdown :as markdown]
            [co.gaiwan.slack.markdown.render-text :as render-text]))

(defn channel-link [org-name channel-id]
  (str "https://" org-name ".slack.com/archives/" channel-id))

(defn user-profile-link [org-name user-id]
  (str "https://" org-name ".slack.com/team/" user-id))

;; TODO: move to domain.message
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

(defn enrich-message
  ""
  [message {:keys [users handlers text-handlers user-keys org-name]
            :or   {user-keys [:user-profile/image-48
                              :user/name
                              :user/real-name
                              :user-profile/display-name]}}]
  (let [user-id    (:message/user-id message)
        user       (get users user-id)
        channel-id (:message/channel-id message)
        timestamp  (:message/timestamp message)
        inst       (when (contains? message :message/timestamp)
                     (time-util/ts->inst timestamp))]
    (try
      (cond-> message
        user
        (merge (select-keys user user-keys))

        (contains? message :message/text)
        (assoc :message/hiccup (markdown/markdown->hiccup (:message/text message) {:handlers handlers}))

        (and text-handlers (contains? message :message/text))
        (render-text/enrich-message (:user-id text-handlers)
                                    (:channel-id text-handlers))

        inst
        (assoc :message/time (time-util/format-inst-time inst))

        :->
        (-> (assoc :message/permalink (permalink org-name channel-id timestamp)
                   :channel/link (channel-link org-name channel-id)
                   :user-profile/link (user-profile-link org-name user-id)
                   :user-profile/display-name (actual-display-name message user))))
      (catch Exception e
        (prn message)
        (throw e)))))

(defn enrich
  ""
  ([message-tree]
   (enrich message-tree nil))
  ([message-tree opts]
   (update-vals message-tree #(enrich-message % opts))))

(defn enrich-entries
  "Like [[enrich]], but instead of enriching all messages, only enrich the ones
  with the given timestamps."
  ([message-tree timestamps]
   (enrich-entries message-tree timestamps nil))
  ([message-tree timestamps opts]
   (let [get-effected-messages-from-tree (fn [mt ts]
                                           (update mt ts #(enrich-message % opts)))]
     (reduce get-effected-messages-from-tree message-tree timestamps))))


(comment
  (markdown/markdown->hiccup "*hey* I :wink: am  <@U03280RER7B>"
                             {:handlers
                              {:emoji (fn [[_ emoji] _]
                                        [:slack-widgets/emoji {:name emoji}])
                               :user-id (fn [[_ user-id] _]
                                          [:slack-widgets/user-mention {:id user-id}])}})
  ;; ([:b "hey"] " I am  " [:span.username [:em "<" "U03280RER7B" ">"]])
  )
