(ns co.gaiwan.slack.markdown
  (:require [co.gaiwan.slack.markdown.parser :as parser]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn img-url->emoji
  "Parse the unicode information out of slack's image links and turn it into an
  emoji or emoji sequence.

  Slack's image links all contain something like 1f482-200d-2642-fe0f which is
  really just the code points in hex."
  [url]
  (try
    (or
     (->> (str/split (ffirst (re-seq #"[0-9a-f]{4,5}([-/][0-9a-f]{4,5})*" url)) #"-")
          (map #(Character/toString (Long/parseLong % 16)))
          (apply str))
     url)
    (catch Exception e
      url)))

(def standard-emoji-map
  "A map from emoji text to emoji.
  `(text->emoji \"smile\") ;; => \"https://..png\"`"
  (delay
    (with-open [r (io/reader (io/resource "clj-slack/emojis.json"))]
      (into {}
            (map (juxt key (comp img-url->emoji val)))
            (json/read r)))))

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
  (walk/prewalk
   (fn [[type content :as token]]
     (if (= :emoji type)
       [:span.emoji (or (text->emoji content emoji-map)
                        (str ":" content ":"))]
       token))
   message))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Hiccup

(def segment->hiccup nil)

(defmulti segment->hiccup
  "Convert a single parsed segment of the form [type content] to hiccup."
  (fn [[type] handlers] type))

(defn segments->hiccup [segments handlers]
  (cond
    (string? segments)
    segments

    (and (vector? segments) (keyword? (first segments)))
    ((get handlers (first segments) segment->hiccup) segments handlers)

    (seqable? segments)
    (map #(segments->hiccup % handlers) segments)

    :else
    segments))

(defmethod segment->hiccup :default [[_ content] handlers]
  content)

(defmethod segment->hiccup :code-block [[_ content] handlers]
  [:pre.highlight [:code [:lambdaisland.hiccup/unsafe-html content]]])

(defmethod segment->hiccup :inline-code [[_ content] handlers]
  [:code [:lambdaisland.hiccup/unsafe-html content]])

(defmethod segment->hiccup :user-id [[_ user-id] handlers]
  [:span.username [:em "<" user-id ">"]])

(defmethod segment->hiccup :channel-id [[_ channel-id channel-name] handlers]
  [:span.channel [:i "#" (or channel-name channel-id) ">"]])

(defmethod segment->hiccup :emoji [[_ code] handlers]
  [:span.emoji code])

(defmethod segment->hiccup :bold [[_ content] handlers]
  [:b (segments->hiccup content handlers)])

(defmethod segment->hiccup :italic [[_ content] handlers]
  [:i (segments->hiccup content handlers)])

(defmethod segment->hiccup :strike-through [[_ content :as segment] handlers]
  [:del (segments->hiccup content handlers)])

(defmethod segment->hiccup :url [[_ content] handlers]
  [:a {:href content} content])

(defn markdown->hiccup
  ([md]
   (markdown->hiccup md nil))
  ([md {:keys [handlers]}]
   (-> (parser/parse md)
       (segments->hiccup handlers))))
