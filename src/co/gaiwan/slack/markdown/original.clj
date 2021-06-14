(ns co.gaiwan.slack.markdown.original
  (:require [co.gaiwan.slack.markdown.parser :as parser]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [hiccup2.core :as hiccup]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn- extract-texts
  [messages]
  (let [texts (map :text messages)]
    (str/join " " texts)))

(defn- parse-users
  [texts]
  (re-seq #"(?<=<@)[^\|>]+" texts))

(defn extract-user-ids
  "Given a seq of slack messages, return user ids mentioned."
  [messages]
  (into #{} (comp
             (map :text)
             (map parse-users)
             cat)
        messages))

(defn replace-ids-names
  "Replaces user/slack-id with user/name.
  Message is a vector of vectors format returned by parser/parse."
  [message id-names]
  (walk/postwalk
   (fn [token]
     (if (and (vector? token) (= :user-id (first token)))
       (let [user-id (second token)]
         [:user {:user-id user-id :user-name (get id-names user-id user-id)}])
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

(defn replace-custom-emojis
  "Replace `:emoji:` with unicode or img tag."
  [message emoji-map]
  (map (fn [[type content :as token]]
         (if (= :emoji type)
           [:emoji (or (text->emoji content emoji-map)
                       (str ":" content ":"))]
           token))
       message))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hiccup

(defmulti segment->hiccup
  "Convert a single parsed segment of the form [type content] to hiccup."
  first)

(defn segments->hiccup [segments]
  (cond
    (string? segments)
    segments

    (and (vector? segments) (keyword? (first segments)))
    (segment->hiccup segments)

    (seqable? segments)
    (map segment->hiccup segments)

    :else
    segments))

(defmethod segment->hiccup :default [[type content]]
  content)

(defmethod segment->hiccup :code-block [[type content]]
  [:pre.highlight [:code (hiccup/raw content)]])

(defmethod segment->hiccup :inline-code [[type content]]
  [:code (hiccup/raw content)])

(defmethod segment->hiccup :user [[type content]]
  ;; FIXME: don't hardcode this
  [:span.username [:a {:href (str "https://someteam.slack.com/team/" (:user-id content))}
                   "@" (:user-name content)]])

(defmethod segment->hiccup :channel-id [[type content name]]
  [:i "#" (if-not (empty? name)
            name
            content)])

(defmethod segment->hiccup :emoji [[type content]]
  [:span.emoji content])

(defmethod segment->hiccup :bold [[type content]]
  [:b (segments->hiccup content)])

(defmethod segment->hiccup :italic [[type content]]
  [:i (segments->hiccup content)])

(defmethod segment->hiccup :strike-through [[type content :as segment]]
  [:del (segments->hiccup content)])

(defmethod segment->hiccup :url [[type content]]
  [:a {:href content} content])

(defn message->hiccup
  "Parse slack markup and convert to hiccup."
  ([message usernames]
   (message->hiccup message usernames {}))
  ([message usernames emojis]
   [:p (segments->hiccup
        (-> message
            (parser/parse2)
            (replace-ids-names usernames)
            (replace-custom-emojis emojis)))]))
