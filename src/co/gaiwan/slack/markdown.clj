(ns co.gaiwan.slack.markdown
  "Convert slack-flavored Markdown to Hiccup. This namespace contains the main
  plumbing to recursively transform our parse tree to Hiccup, and base
  implementations for the different types of elements. If you need specific
  handling, e.g. for username rendering, then you pass in context-specific
  handlers."
  (:require [co.gaiwan.slack.markdown.parser :as parser]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [charred.api :as json]))


(defn hex->char
  "Take a string of unicode characters specified in hexadecimal with dashes, and
  return the actual unicode character(s)"[hex]
  (->> (str/split (ffirst (re-seq #"[0-9a-f]{4,5}([-/][0-9a-f]{4,5})*" hex)) #"-")
       (map #(Character/toString (Long/parseLong % 16)))
       (apply str)))

(defn skin-tone-mapping
  "Map from unicode sequence to unicode-sequence-with-X-placeholder, e.g.

  1f469-200d-1f527 -> 1f469-X-200d-1f527

  The emoji on the left can be given a skin tone by replacing the X with a skin tone modifier.
  "
  []
  (with-open [r (io/reader (io/resource "clj-slack/skin_tones.json"))]
    (into {} (map (fn [s]
                    [(str/replace s #"-1f3f[b-f]" "")
                     (str/replace s #"-1f3f[b-f]" "-X")])
                  (json/read-json r)))))

(def standard-emoji-map
  "A map from emoji text to emoji.
  `(text->emoji \"smile\") ;; => \"https://..png\"`"
  (delay
    (let [skin-tones (skin-tone-mapping)]
      (into {}
            (concat
             ;; Best we've managed to do so far, yank the emoji data right out
             ;; of slack's JS sources, then combine with the unicode list of
             ;; characters that take skin tone modifiers.
             (with-open [r (io/reader (io/resource "clj-slack/emojis_raw.json"))]
               (mapcat (fn [{:strs [name unicode]}]
                         (let [skin-tone-pattern (get skin-tones unicode)]
                           (cond-> [[name (hex->char unicode)]]
                             skin-tone-pattern
                             (concat
                              (map
                               (fn [[suffix modifier]]
                                 [(str name "::" suffix) (hex->char (str/replace skin-tone-pattern #"X" modifier))])
                               {"skin-tone-2" "1f3fb"
                                "skin-tone-3" "1f3fc"
                                "skin-tone-4" "1f3fd"
                                "skin-tone-5" "1f3fe"
                                "skin-tone-6" "1f3ff"})))))
                       (vals (json/read-json r)))))))))

(comment
  ;; for checking in a browser
  (spit "/tmp/xxx"
        (pr-str
         @standard-emoji-map)))

(defn text->emoji
  "Convert a shortcode like `:woman-running:` into something we can render. Will
  return an `[:img ...]` for custom emojis, or a unicode character (or character
  sequence) for standard emojis. Takes an optional emoji-map to be used in
  addition to the standard built-ins."
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

(defmulti segment->hiccup
  "Convert a single parsed segment of the form [type content] to hiccup. Handlers
  are recursively passed down, and can be used to override rendering of certain
  segment types."
  (fn [[type] handlers] type))

(defn segments->hiccup
  "Convert a collection/sequence of segments to Hiccup."
  [segments handlers]
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
  "Take a markdown string and convert it to Hiccup.

  `handlers` is an optional map of keyword -> function, which will override the
  rendering of a specific type of segment."
  ([md]
   (markdown->hiccup md nil))
  ([md {:keys [handlers]}]
   (-> (parser/parse md)
       (segments->hiccup handlers))))
