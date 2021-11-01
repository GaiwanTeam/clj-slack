(ns co.gaiwan.slack.markdown.parser-test
  (:require [clojure.test :refer :all]
            [co.gaiwan.slack.markdown.parser :as mp]))

(deftest slack-advanced-markdown-test
  (testing "Unordered list"
    (is (= [[:undecorated "•   list\n•   list\n•   list\n"]]
           (mp/parse "•   list\n•   list\n•   list\n"))))
  (testing "Ordered list"
    (is (= [[:undecorated "1.  list\n2.  list\n3.  list\n"]]
           (mp/parse "1.  list\n2.  list\n3.  list\n")))))

(deftest test-parse
  (testing "basic messages"
    (is (= [[:undecorated "This is a normal message"]]
           (mp/parse "This is a normal message")))
    (is (= [[:user-id "U4F2A0Z8ER"]]
           (mp/parse "<@U4F2A0Z8ER>")))
    (is (= [[:channel-id "C4F2A26SGSHBW"]]
           (mp/parse "<#C4F2A26SGSHBW>")))
    (is (= [[:channel-id "C03S1L9DN" "clojurescript"]]
           (mp/parse "<#C03S1L9DN|clojurescript>")))
    (is (= [[:inline-code "DateTime"]]
           (mp/parse "`DateTime`")))
    (is (= [[:code-block "(some clojure code)"]]
           (mp/parse "```(some clojure code)```")))
    (is (= [[:bold "hey!"]]
           (mp/parse "*hey!*")))
    (is (= [[:italic "hello"]]
           (mp/parse "_hello_")))
    (is (= [[:emoji "thumbsup"]]
           (mp/parse ":thumbsup:")))
    (is (= [[:emoji "+1"]]
           (mp/parse ":+1:")))
    (is (= [[:emoji "-1"]]
           (mp/parse ":-1:")))
    (is (= [[:emoji "e-mail"]]
           (mp/parse ":e-mail:")))
    (is (= [[:bold "hi!"] [:undecorated " "] [:emoji "smiles"]]
           (mp/parse "*hi!* :smiles:")))
    (is (= [[:undecorated "12:34:56:78:90:12:34:56:78:90:12:34:56:78:90:12"]]
           (mp/parse "12:34:56:78:90:12:34:56:78:90:12:34:56:78:90:12")))
    (is (= [[:strike-through "strike-through"]]
           (mp/parse "~strike-through~")))
    (is (= [[:undecorated "just_some_snake_case"]]
           (mp/parse "just_some_snake_case")))
    (is (= [[:url "https://google.com"]]
           (mp/parse "<https://google.com>")))
    (is (= [[:undecorated "from: "]
            [:url "https://google.com"]]
           (mp/parse "from: <https://google.com>")))
    (is (= [[:undecorated "&<>"]]
           (mp/parse "&amp;&lt;&gt;"))))

  (testing "nested regions"
    ;; Basic case
    (is (= [[:bold "hello"]]
           (mp/parse "*hello*")))

    ;; two unrelated regions
    (is (= [[:italic "hello"] [:undecorated " "] [:bold "world"]]
           (mp/parse "_hello_ *world*")))

    ;; single nested case
    (is (= [[:italic [:bold "hello"]]]
           (mp/parse "_*hello*_")))

    ;; undecorated text outside regions
    (is (= [[:bold "hello"] [:undecorated " world again"]]
           (mp/parse "*hello* world again")))

    (is (= [[:undecorated "hello "] [:bold "world"] [:undecorated " again"]]
           (mp/parse "hello *world* again")))

    (is (= [[:undecorated "hello world "] [:bold "again"]]
           (mp/parse "hello world *again*")))

    ;; undecorated text inside regions
    (is (= [[:italic [[:bold "hello"] [:undecorated " world"]]]]
           (mp/parse "_*hello* world_")))

    (is (= [[:italic [[:undecorated "hello "] [:bold "world"]]]]
           (mp/parse "_hello *world*_")))

    (is (= [[:italic [[:bold "hello"] [:undecorated " world again"]]]]
           (mp/parse "_*hello* world again_")))

    (is (= [[:italic [[:undecorated "hello "] [:bold "world"] [:undecorated " again"]]]]
           (mp/parse "_hello *world* again_")))

    (is (= [[:italic [[:undecorated "hello world "] [:bold "again"]]]]
           (mp/parse "_hello world *again*_")))

    ;; Two nested regions
    (is (= [[:italic [[:bold "hello"] [:undecorated " "] [:strike-through "world"]]]]
           (mp/parse "_*hello* ~world~_"))))

  (testing "No nested regions inside a code block"
    (is (= [[:undecorated "Some text "]
            [:code-block "some code <#C03S1L9DN|clojurescript>"]]
           (mp/parse "Some text ```some code <#C03S1L9DN|clojurescript>```"))))

  (testing "putting it together"
    (let [message "Hey <@U4F2A0Z8ER>: here is the `my-ns.core` code ```
  (let [code 42]
   (inc code))
```
*what do* _you_ *think* :mindblown:
please respond in <#C346HE24SD>"]
      (is (= [[:undecorated "Hey "]
              [:user-id "U4F2A0Z8ER"]
              [:undecorated ": here is the "]
              [:inline-code "my-ns.core"]
              [:undecorated " code "]
              [:code-block "(let [code 42]\n   (inc code))\n"]
              [:undecorated "\n"]
              [:bold "what do"]
              [:undecorated " "]
              [:italic "you"]
              [:undecorated " "]
              [:bold "think"]
              [:undecorated " "]
              [:emoji "mindblown"]
              [:undecorated "\nplease respond in "]
              [:channel-id "C346HE24SD"]]
             (mp/parse message))))))

;; Try out Slack message parsing at
;; https://api.slack.com/docs/messages/builder?msg=%7B%22text%22%3A%22xx1_%20*basic*%60%22%7D
(deftest parse-test
  (are [x y] (= y (mp/parse x))
    "basic"              [[:undecorated "basic"]]
    "*bold*"             [[:bold "bold"]]
    "basic *bold* basic" [[:undecorated "basic "] [:bold "bold"] [:undecorated " basic"]]
    "basic *basic"       [[:undecorated "basic *basic"]]
    "_italic_"           [[:italic "italic"]]
    "xx _italic_ xx"     [[:undecorated "xx "] [:italic "italic"] [:undecorated " xx"]]
    "xx_oops_"           [[:undecorated "xx_oops_"]]
    "xx1*basic*"         [[:undecorated "xx1*basic*"]]
    "xx1`*basic*`"       [[:undecorated "xx1`*basic*`"]]
    "xx1_`*basic*`"      [[:undecorated "xx1_"] [:inline-code "*basic*"]]
    "xx1_ *basic*`"      [[:undecorated "xx1_ *basic*`"]]
    "> foo *_bar_*"      [[:blockquote [[:undecorated "foo "] [:bold [:italic "bar"]]]]]
    ">foo *_bar_*"       [[:blockquote [[:undecorated "foo "] [:bold [:italic "bar"]]]]]
    ">>foo *_bar_*"      [[:blockquote [[:undecorated ">foo "] [:bold [:italic "bar"]]]]]
    "> >foo *_bar_*"     [[:undecorated "> >foo "] [:bold [:italic "bar"]]]
    ">>>a\nmultiline\nquote" [[:blockquote "a\nmultiline\nquote"]]
    "<http://google.com|Google>" [[:url "http://google.com" "Google"]]
    "<https://clojure.org/reference/multimethods#_isa_based_dispatch>" [[:url "https://clojure.org/reference/multimethods#_isa_based_dispatch"]]))
