(ns co.gaiwan.slack.enrich-test
  (:require [co.gaiwan.slack.enrich :as enrich]
            [co.gaiwan.slack.test-data.raw-events :as raw-events]
            [co.gaiwan.slack.test-data.entities :as entities]
            [co.gaiwan.slack.normalize :as normalize]
            [co.gaiwan.slack.markdown :as markdown]
            [clojure.test :refer [deftest testing is are run-tests]]))

(defn user-id-handler [[_ user-id] _]
  [:span.username
   [:a {:href (str "https://gaiwanteam.slack.com/team/" user-id)}
    "@" (some #(when (= user-id (:user/id %))
                 (:user/real-name %))
              entities/users-normalized)]])

(defn emoji-handler [[_ code] _]
  [:span.emoji (get @markdown/standard-emoji-map code code)])

(deftest enrich-message-test
  (is (= {:message/timestamp "1621508880.437600",
          :message/hiccup
          '("Thanks so, so much "
            [:span.username
             [:a {:href "https://gaiwanteam.slack.com/team/U01G7GP6L5B"}
              "@" "Arne Brasseur"]]
            "!! That was awesome"),
          :user-profile/link "https://gaiwanteam.slack.com/team/U01FVSUGVN3",
          :message/channel-id "C015DQFEGMT",
          :message/time "11:05:00",
          :channel/link "https://gaiwanteam.slack.com/archives/C015DQFEGMT",
          :user-profile/display-name "U01FVSUGVN3",
          :message/permalink "https://gaiwanteam.slack.com/archives/C015DQFEGMT/p1621508880437600",
          :message/user-id "U01FVSUGVN3"}
         (enrich/enrich-message
          {:message/timestamp "1621508880.437600",
           :message/text "Thanks so, so much <@U01G7GP6L5B>!! That was awesome",
           :message/channel-id "C015DQFEGMT",
           :message/user-id "U01FVSUGVN3"}
          {:users entities/users-normalized
           :org-name "gaiwanteam"
           :handlers {:user-id user-id-handler
                      :emoji emoji-handler}}))))

(deftest enrich-entries-test
  (is (= {"1652772696.340349" {:message/timestamp "1652772696.340349"
                               :message/text "test"
                               :message/channel-id "C064BA6G2"
                               :message/user-id "U01FVSUGVN3"}
          "1652253142.426959" {:message/timestamp "1652253142.426959"
                               :message/text "hello"
                               :message/channel-id "C064BA6G2"
                               :message/user-id "U01G7GP6L5B"}
          "1621508880.437600" {:message/timestamp "1621508880.437600"
                               :message/hiccup ["testing father"]
                               :user/name "ariel"
                               :user-profile/link "https://gaiwanteam.slack.com/team/U03280RER7B"
                               :user-profile/image-48 "https://avatars.slack-edge.com/2022-02-07/3060345905878_07a3c0e40679cd03aec1_48.png"
                               :message/channel-id "C064BA6G2"
                               :message/time "11:05:00"
                               :channel/link "https://gaiwanteam.slack.com/archives/C064BA6G2"
                               :user-profile/display-name "Ariel Alexi"
                               :message/permalink "https://gaiwanteam.slack.com/archives/C064BA6G2/p1621508880437600"
                               :message/user-id "U03280RER7B"
                               :user/real-name "Ariel Alexi"}}
         (enrich/enrich-entries raw-events/simple-message-tree
                                ["1621508880.437600"]
                                {:users (into {}
                                              (map (juxt :user/id identity))
                                              entities/users-normalized)
                                 :org-name "gaiwanteam"}))))
