(ns co.gaiwan.message.markdown-test
  (:require [clojure.test :refer :all]
            [co.gaiwan.message.markdown :as markdown]))

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
             (markdown/message->hiccup "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR>" {"U4F2A0Z8ER" "John" "U4F2A0Z9HR" "Marry"}))))))

(deftest markdown-utilities
  (testing "extract-user-ids"
    (is (= #{"U4F2A0Z9HR" "U4F2A0Z8ER"}
           (markdown/extract-user-ids (list {:text "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR>: here is the `my-ns.core` code"})))))
  (testing "emoji"
    (is (= (list "woman_firefighter" "rainbow" "mahjong")
           (take 3 (keys @markdown/standard-emoji-map))))))
