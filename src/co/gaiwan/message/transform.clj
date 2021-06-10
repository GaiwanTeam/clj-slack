(ns co.gaiwan.message.transform
  (:require [clojure.walk :as walk]
            [hiccup2.core :as hiccup]))

;; Transformers ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn- transform-undecorated
  [content]
  content)

(defn- transform-code-block
  [content]
  [:pre.highlight [:code (hiccup/raw content)]])

(defn- transform-inline-code
  [content]
  [:code (hiccup/raw content)])

(defn- transform-user-id
  [content]
  [:user-id content])

(defn- transform-channel-id
  [content name]
  [:i "#" (if-not (empty? name)
            name
            content)])

(defn- transform-emoji
  [content]
  [:span.emoji content])

(defn- transform-bold
  [content]
  [:b content])

(defn- transform-italic
  [content]
  [:i content])

(defn- transform-strike-through
  [content]
  [:del content])

(defn- transform-url
  [content]
  [:a {:href content} content])

(def transformations {:undecorated transform-undecorated
                      :code-block transform-code-block
                      :inline-code transform-inline-code
                      :user-id transform-user-id
                      :channel-id transform-channel-id
                      :emoji transform-emoji
                      :bold transform-bold
                      :italic transform-italic
                      :strike-through transform-strike-through
                      :url transform-url})

(defn- second-or-rest [coll]
  (if (= (count coll) 2)
    (second coll)
    (rest coll)))

(defn- transform-segments [segments]
  (cond
    (string? segments)
    segments

    (and (vector? segments) (keyword? (first segments)))
    (if-let [t (transformations (first segments))]
      (t (second-or-rest segments))
      (second-or-rest segments))

    (vector? segments)
    (apply list segments)

    :else
    segments))

;; API ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn tree->hiccup
  [tree]
  (walk/postwalk transform-segments tree))

