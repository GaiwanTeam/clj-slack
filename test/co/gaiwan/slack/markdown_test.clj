(ns co.gaiwan.slack.markdown-test
  (:require [clojure.test :refer :all]
            [co.gaiwan.slack.markdown :as markdown]
            [co.gaiwan.slack.test-data.markdown :as md-data]
            [co.gaiwan.slack.test-data.entities :as entity-data]))

(defn user-id-handler [[_ user-id] _]
  [:span.username
   [:a {:href (str "https://someteam.slack.com/team/" user-id)}
    "@" (get entity-data/user-map user-id user-id)]])

(defn emoji-handler [[_ code] _]
  [:span.emoji (get @markdown/standard-emoji-map code code)])

(deftest markdown->hiccup-test
  (testing "bold-italic-del"
    (is (= ["This is a rich text testing:\n1. "
            [:b "bold face"]
            "\n2. "
            [:i "italics"]
            "\n3. "
            [:del "strike"]
            "\n\norder test\na\nb"]
           (markdown/markdown->hiccup md-data/bold-italic-del nil)))
    (is (= ["Hey "
            [:span.username
             [:a {:href "https://someteam.slack.com/team/U4F2A0Z8ER"} "@" "John"]]
            " "
            [:span.username
             [:a {:href "https://someteam.slack.com/team/U4F2A0Z9HR"} "@" "Marry"]]]
           (markdown/markdown->hiccup md-data/user-references {:handlers {:user-id user-id-handler
                                                                          :emoji emoji-handler}})))
    (is (= [[:b
             "Hey everyone, we’re so excited to be here for DevOps Enterprise Summit talking about"]
            "\n"
            [:span.emoji "➡️"]
            " "
            [:i
             [:b
              (list "Be sure to visit our booth "
                    [:a
                     {:href "https://doesvirtual.com/teamform"}
                     "https://doesvirtual.com/teamform"])]]
            " \n"
            [:span.emoji "📺"]
            " "
            [:i
             [:b
              (list "Or join us anytime on Zoom - "
                    [:a {:href "https://bit.ly/3iIdX1X"} "https://bit.ly/3iIdX1X"])]]
            "\n"
            [:span.emoji "📣"]
            " "
            [:i
             [:b
              (list "Schedule a private demo - "
                    [:a
                     {:href "https://teamform.co/demo"}
                     "https://teamform.co/demo"])]]
            "\n"
            [:span.emoji "🎁"]
            " "
            [:i
             [:b
              (list "Register for giveaway (1x PS5 or XBox Series X, 1 x 50min chat with the authors of Team Topologies, 20x IT Rev Books) "
                    [:a
                     {:href "https://www.teamform.co/does-giveaway"}
                     "https://www.teamform.co/does-giveaway"])]]
            "\n\n"
            [:b
             "We’ve got a exciting week with a bunch of demos of TeamForm scheduled"]
            "\n"
            [:span.emoji "⭐"]
            " 11-11:15am PDT: TeamForm Live Demo: Managing Supply & Demand at Scale - join @ "
            [:a
             {:href "https://us02web.zoom.us/j/81956904920"}
             "https://us02web.zoom.us/j/81956904920"]
            "\n"
            [:span.emoji "⭐"]
            " 12:45-1:00pm PDT: TeamForm Live Demo: Measuring Team Organising Principles - join @ "
            [:a
             {:href "https://us02web.zoom.us/j/81956904920"}
             "https://us02web.zoom.us/j/81956904920"]
            "\n"
            [:span.emoji "📊"]
            " 3:45-4pm PDT: TeamForm Live Demo: Measuring Team Proficiency - join @ "
            [:a
             {:href "https://us02web.zoom.us/j/81956904920"}
             "https://us02web.zoom.us/j/81956904920"]
            "\n\nLater this week:\n"
            [:span.emoji "➡️"]
            " Register for our AMA with Authors of TeamTopologies "
            [:a {:href "https://sched.co/ej42"} "https://sched.co/ej42"]
            " with "
            [:span.username
             [:a
              {:href "https://someteam.slack.com/team/ULTTZCP7S"}
              "@"
              "ULTTZCP7S"]]
            " & "
            [:span.username
             [:a
              {:href "https://someteam.slack.com/team/UBE001UAX"}
              "@"
              "UBE001UAX"]]]
           (markdown/markdown->hiccup md-data/complicated-message {:handlers
                                                                   {:user-id user-id-handler
                                                                    :emoji emoji-handler}})))))

(deftest markdown-utilities
  (testing "extract-user-ids"
    (is (= #{"U4F2A0Z9HR" "U4F2A0Z8ER"}
           (markdown/extract-user-ids (list {:text "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR>: here is the `my-ns.core` code"})))))
  (testing "emoji"
    (is (= ["+1"
            "+1::skin-tone-2"
            "+1::skin-tone-3"
            "+1::skin-tone-4"
            "-1"
            "-1::skin-tone-2"
            "-1::skin-tone-3"
            "-1::skin-tone-4"
            "100"
            "1234"
            "8ball"
            "a"
            "ab"
            "abacus"]
           (take 14 (sort (keys @markdown/standard-emoji-map)))))))
