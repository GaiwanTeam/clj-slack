(ns co.gaiwan.slack.markdown.render-text
  "Basic \"rendering\" of markdown, replacing channel and user ids with something
  more readable"
  (:require [clojure.string :as str]))


(defn render-text [text user-handler channel-handler]
  (when text
    (-> text
        (str/replace #"<@([A-Z0-9]+)>"
                     (fn [[_ uid]]
                       (or (user-handler uid) uid)))
        (str/replace #"<#([A-Z0-9]+)(\|[^>]*)>"
                     (fn [[_ cid]]
                       (or (channel-handler cid) cid))))))

(defn enrich-message [message user-handler channel-handler]
  (assoc message
         :message/text-rendered
         (render-text
          (:message/text message)
          user-handler
          channel-handler)))
