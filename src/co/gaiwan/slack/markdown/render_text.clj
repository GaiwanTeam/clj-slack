(ns co.gaiwan.slack.markdown.render-text
  "Basic \"rendering\" of markdown, replacing channel and user ids with something
  more readable"
  (:require [clojure.string :as str]
            [co.gaiwan.slack.markdown :as markdown]))


(defn render-text [text user-handler channel-handler]
  (when text
    (-> text
        (str/replace #"<@([A-Z0-9]+)>"
                     (fn [[_ uid]]
                       (or (user-handler uid) uid)))
        (str/replace #"<#([A-Z0-9]+)(\|[^>]*)>"
                     (fn [[_ cid]]
                       (or (channel-handler cid) cid)))
        (str/replace #":([a-z0-9_+-]{1,55}(::skin-tone-[2-6])?):"
                     (fn [[orig code]]
                       (get @markdown/standard-emoji-map code orig))))))

(defn enrich-message [message user-handler channel-handler]
  (assoc message
         :message/text-rendered
         (render-text
          (:message/text message)
          user-handler
          channel-handler)))
