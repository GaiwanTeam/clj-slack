(ns co.gaiwan.slack.markdown.core
  (:require [co.gaiwan.slack.markdown.parser :as parser]
            [co.gaiwan.slack.markdown.transform :as transform]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn- parse-users
  [texts]
  (re-seq #"(?<=<@)[^\|>]+" texts))

(defn- replace-ids-names
  "Replaces user/slack-id with user/name.
  Message is a vector of vectors format returned by parser/parse."
  [message id-names]
  (walk/postwalk
   (fn [token]
     (if (and (vector? token) (= :user-id (first token)))
       (let [user-id (second token)
             user-name (get id-names user-id user-id)]
         ;; FIXME: don't hardcode this
         [:span.username [:a {:href (str "https://someteam.slack.com/team/" user-id)}
                          "@" user-name]])
       token))
   message))

(def standard-emoji-map
  "A map from emoji text to emoji.
  `(text->emoji \"smile\") ;; => \"😄\"`"
  (delay (with-open [r (io/reader (io/resource "emojis.json"))]
           (let [emoji-list (json/read r :key-fn keyword)]
             (into {}
                   (for [{:keys [emoji aliases]} emoji-list
                         alias aliases]
                     [alias emoji]))))))

(defn text->emoji
  ([text]
   (text->emoji text {}))
  ([text emoji-map]
   (let [emoji-map (merge @standard-emoji-map emoji-map)]
     (loop [shortcode text]
       (when-let [link (emoji-map shortcode)]
         (cond
           (str/starts-with? link "alias:")
           (recur (str/replace-first link #"alias:" ""))

           (str/starts-with? link "https:")
           [:img {:alt text :src link}]

           ;; return plaintext when we have unicode
           ;; or just nil
           :else link))))))

(defn- replace-custom-emojis
  "Replace `:emoji:` with unicode or img tag."
  [message emoji-map]
  (map
   (fn [[type content :as token]]
     (if (= :emoji type)
       [:span.emoji (or (text->emoji content emoji-map)
                        (str ":" content ":"))]
       token))
   message))

;; API ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn extract-user-ids
  "Given a seq of slack messages, return user ids mentioned."
  [messages]
  (into #{} (comp
             (map :text)
             (map parse-users)
             cat)
        messages))

(defn message->hiccup
  "Parse slack markup and convert to hiccup."
  ([message usernames]
   (message->hiccup message usernames {}))
  ([message usernames emojis]
   [:p (-> message
           (parser/parse2)
           (transform/tree->hiccup)
           (replace-ids-names usernames)
           (replace-custom-emojis emojis))]))

(comment
  (def t "*Hey everyone, we\u2019re so excited to be here for DevOps Enterprise Summit talking about*\n:arrow_right: _*Be sure to visit our booth <https://doesvirtual.com/teamform>*_ \n:tv: _*Or join us anytime on Zoom -\u00a0<https://bit.ly/3iIdX1X>*_\n:mega: _*Schedule a private demo - <https://teamform.co/demo>*_\n:gift: _*Register for giveaway (1x PS5 or XBox Series X, 1 x 50min chat with the authors of Team Topologies, 20x IT Rev Books) <https://www.teamform.co/does-giveaway>*_\n\n*We\u2019ve got a exciting week with a bunch of demos of TeamForm scheduled*\n:star: 11-11:15am PDT: TeamForm Live Demo: Managing Supply &amp; Demand at Scale - join @ <https://us02web.zoom.us/j/81956904920>\n:star: 12:45-1:00pm PDT: TeamForm Live Demo: Measuring Team Organising Principles - join @ <https://us02web.zoom.us/j/81956904920>\n:bar_chart: 3:45-4pm PDT: TeamForm Live Demo: Measuring Team Proficiency - join @ <https://us02web.zoom.us/j/81956904920>\n\nLater this week:\n:arrow_right: Register for our AMA with Authors of TeamTopologies <https://sched.co/ej42> with <@ULTTZCP7S> &amp; <@UBE001UAX>")
  (transform/tree->hiccup (parser/parse2 t)))
