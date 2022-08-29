(ns repl-sessions.affected-messages
  (:require [co.gaiwan.slack.normalize.messages :as messages]
            [co.gaiwan.slack.test-data.raw-events :as raw-events]
            [co.gaiwan.slack.markdown :as markdown]
            [co.gaiwan.slack.test-data.markdown :as md-data]
            [co.gaiwan.slack.test-data.entities :as entity-data]
            [co.gaiwan.slack.markdown.parser :as parser]
            [co.gaiwan.slack.enrich :as enrich]
            [co.gaiwan.slack.api :as api]
            [co.gaiwan.slack.normalize.web-api :as norm-web]
            ))

;; https://linear.app/gaiwan/issue/ITR-41

(messages/add-event
 {}
 {"ts" "1602745198.022400",
  "user" "U010ACDMUHX",
  "text" "First message",
  "type" "message",
  "channel" "C7YF1SBT3",
  "team" "T03RZGPFR"}
 )
;; => {"1602745198.022400"
;;     #:message{:timestamp "1602745198.022400",
;;               :text "First message",
;;               :channel-id "C7YF1SBT3",
;;               :user-id "U010ACDMUHX"}}


(reduce messages/add-event {} raw-events/multiple-days-pacific)
(tap> (reduce messages/add-event {} raw-events/replies+broadcast))

md-data/user-references

(defn user-id-handler [[_ user-id] _]
  [:span.username
   [:a {:href (str "https://someteam.slack.com/team/" user-id)}
    "@" (get entity-data/user-map user-id user-id)]])

(defn emoji-handler [[_ code] _]
  [:span.emoji (get @markdown/standard-emoji-map code code)])

(defstyled slack-emoji :div
  ,,,)

(markdown/markdown->hiccup
 "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR> :curly_haired_man: :cityscape:
```
(+ 1 1)
```"
 {:handlers {:user-id user-id-handler
             :emoji emoji-handler
             :code-block (fn [[_ code] _]
                           [slack-emoji code])}})

(parser/parse
 "Hey <@U4F2A0Z8ER> <@U4F2A0Z9HR> :curly_haired_man: :cityscape:
```
(+ 1 1)
```"
 )


(def api-conn (api/conn   #_(System/getenv "SLACK_API_TOKEN")))

(def channels (api/conversations api-conn))
(def users (api/users api-conn))

(def events
  (api/history api-conn
               {:channel
                (some #(when (= "random" (:name_normalized %))
                         (:id %)) channels)}))

(tap> events)

(tap> (map norm-web/user+profile (get users "members") ))

(tap> users)


(def message-tree (reduce messages/add-event {} (take 50 (remove #(get % "bot_id")
                                                                 (get events "messages")))))

(tap> message-tree)
(tap> users)
(def enriched
  (enrich/enrich message-tree
                 {:users (into {}
                               (map (juxt :user/id identity))
                               (map norm-web/user+profile (get users "members") ))
                  :org-name "gaiwanteam"}
                 ))

[
 (get message-tree "1620399358.006300")
 (get enriched "1620399358.006300")]
