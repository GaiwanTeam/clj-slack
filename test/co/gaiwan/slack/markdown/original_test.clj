(ns co.gaiwan.slack.markdown.original-test
  (:require [clojure.test :refer :all]
            [co.gaiwan.slack.markdown.original :as markdown]))

(deftest message->hiccup
  (testing "bold-italic-del"
    (let [bold-italic-del "This is a rich text testing:\n1. *bold face*\n2. _italics_\n3. ~strike~\n\norder test\na\nb"]
      (is (= [:p
              ["This is a rich text testing:\n1. "
               [:b "bold face"]
               "\n2. "
               [:i "italics"]
               "\n3. "
               [:del "strike"]
               "\n\norder test\na\nb"]]
             (markdown/message->hiccup bold-italic-del nil)))
      (is (= [:p
              (list "Hey "
                    [:span.username
                     [:a {:href "https://someteam.slack.com/team/U4F2A0Z8ER"} "@" "John"]]
                    " "
                    [:span.username
                     [:a {:href "https://someteam.slack.com/team/U4F2A0Z9HR"} "@" "Marry"]])]
             (markdown/message->hiccup "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR>" {"U4F2A0Z8ER" "John" "U4F2A0Z9HR" "Marry"})))
      (is (=
           [:p
            (list [:b
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
                    "UBE001UAX"]])]
           (markdown/message->hiccup
            "*Hey everyone, we\u2019re so excited to be here for DevOps Enterprise Summit talking about*\n:arrow_right: _*Be sure to visit our booth <https://doesvirtual.com/teamform>*_ \n:tv: _*Or join us anytime on Zoom -\u00a0<https://bit.ly/3iIdX1X>*_\n:mega: _*Schedule a private demo - <https://teamform.co/demo>*_\n:gift: _*Register for giveaway (1x PS5 or XBox Series X, 1 x 50min chat with the authors of Team Topologies, 20x IT Rev Books) <https://www.teamform.co/does-giveaway>*_\n\n*We\u2019ve got a exciting week with a bunch of demos of TeamForm scheduled*\n:star: 11-11:15am PDT: TeamForm Live Demo: Managing Supply &amp; Demand at Scale - join @ <https://us02web.zoom.us/j/81956904920>\n:star: 12:45-1:00pm PDT: TeamForm Live Demo: Measuring Team Organising Principles - join @ <https://us02web.zoom.us/j/81956904920>\n:bar_chart: 3:45-4pm PDT: TeamForm Live Demo: Measuring Team Proficiency - join @ <https://us02web.zoom.us/j/81956904920>\n\nLater this week:\n:arrow_right: Register for our AMA with Authors of TeamTopologies <https://sched.co/ej42> with <@ULTTZCP7S> &amp; <@UBE001UAX>" nil))))))

(deftest markdown-utilities
  (testing "extract-user-ids"
    (is (= #{"U4F2A0Z9HR" "U4F2A0Z8ER"}
           (markdown/extract-user-ids (list {:text "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR>: here is the `my-ns.core` code"})))))
  (testing "emoji"
    (is (= (list "woman_firefighter" "rainbow" "mahjong")
           (take 3 (keys @markdown/standard-emoji-map))))))
