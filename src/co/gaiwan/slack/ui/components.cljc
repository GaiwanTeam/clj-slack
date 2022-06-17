(ns co.gaiwan.slack.ui.components
  (:require [clojure.string :as str]))

(defn message [{:keys [org-name]}
               {:message/keys [timestamp
                               channel-id
                               user-id
                               replies
                               hiccup
                               time
                               permalink
                               reactions]
                :user-profile/keys [display-name image-48]
                channel-link :channel/link
                user-profile-link :user-profile/link
                :as msg}]
  ^{:key timestamp}
  [:div.slack-message
   [:div.slack-message-left-box
    [:a.slack-message__profile-pic
     {:href user-profile-link}
     [:img {:src image-48}]]]
   [:div.slack-message-right-box
    [:a.slack-message__username {:href user-profile-link} display-name]
    [:span " "]
    [:span.slack-message__timestamp
     [:a {:rel "nofollow" :href permalink :target "_blank"}
      [:span {:title "Open message in Slack"} time]]]
    [:span.slack-message__content hiccup]
    [:div.slack-message__reactions
     (for [[reaction {:keys [emoji count]}] reactions]
       ^{:key reaction}
       [:div.slack-message__reaction emoji " " count])
     (when (> (count replies) 0)
       [:div.slack-message__reaction
        (count replies)
        " replies"])]]])
