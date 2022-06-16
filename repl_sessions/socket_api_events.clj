(ns repl-sessions.socket-api-events)

;; This is an overview of what events we receive from the socket-api, and what
;; they look like.

(def msgs
  [;; regular message
   {:channel_type "channel",
    :event_ts "1652277544.608089",
    :channel "C020GQ8LW85",
    :type "message",
    :ts "1652277544.608089",
    :team "T01G3T4BVGE",
    :client_msg_id "59e9e2b9-92e5-4e76-a3d3-72274fceaa7c",
    :blocks
    [{:type "rich_text",
      :block_id "lKL",
      :elements
      [{:type "rich_text_section",
        :elements [{:type "text", :text "hellow other world"}]}]}],
    :user "U01G7GP6L5B",
    :text "hellow other world"}

   ;; edit message
   {:channel_type "channel",
    :event_ts "1652278323.014900",
    :channel "C020GQ8LW85",
    :type "message",
    :previous_message
    {:client_msg_id "59e9e2b9-92e5-4e76-a3d3-72274fceaa7c",
     :type "message",
     :text "hellow other world",
     :user "U01G7GP6L5B",
     :ts "1652277544.608089",
     :team "T01G3T4BVGE",
     :blocks
     [{:type "rich_text",
       :block_id "lKL",
       :elements
       [{:type "rich_text_section",
         :elements [{:type "text", :text "hellow other world"}]}]}]},
    :ts "1652278323.014900",
    :hidden true,
    :message
    {:source_team "T01G3T4BVGE",
     :type "message",
     :edited {:user "U01G7GP6L5B", :ts "1652278323.000000"},
     :ts "1652277544.608089",
     :team "T01G3T4BVGE",
     :client_msg_id "59e9e2b9-92e5-4e76-a3d3-72274fceaa7c",
     :blocks
     [{:type "rich_text",
       :block_id "g474T",
       :elements
       [{:type "rich_text_section",
         :elements [{:type "text", :text "hellow other world: saturn"}]}]}],
     :user_team "T01G3T4BVGE",
     :user "U01G7GP6L5B",
     :text "hellow other world: saturn"},
    :subtype "message_changed"}

   ;; remove message
   {:channel_type "channel",
    :event_ts "1652278433.015000",
    :channel "C020GQ8LW85",
    :type "message",
    :previous_message
    {:client_msg_id "7910c333-26b9-4a9b-99ec-be8a07fcfeb6",
     :type "message",
     :text "another one",
     :user "U01G7GP6L5B",
     :ts "1652277481.241669",
     :team "T01G3T4BVGE",
     :blocks
     [{:type "rich_text",
       :block_id "jyjH",
       :elements
       [{:type "rich_text_section",
         :elements [{:type "text", :text "another one"}]}]}]},
    :ts "1652278433.015000",
    :hidden true,
    :deleted_ts "1652277481.241669",
    :subtype "message_deleted"}

   ;; reaction added
   {:type "reaction_added",
    :user "U01G7GP6L5B",
    :reaction "white_check_mark",
    :item {:type "message", :channel "C020GQ8LW85", :ts "1652277544.608089"},
    :item_user "U01G7GP6L5B",
    :event_ts "1652278476.015100"}

   ;; reaction removed
   {:type "reaction_removed",
    :user "U01G7GP6L5B",
    :reaction "white_check_mark",
    :item {:type "message", :channel "C020GQ8LW85", :ts "1652277544.608089"},
    :item_user "U01G7GP6L5B",
    :event_ts "1652278502.015200"}

   ;; thread reply
   {:channel_type "channel",
    :event_ts "1652278538.516289",
    :channel "C020GQ8LW85",
    :type "message",
    :thread_ts "1652277544.608089",
    :ts "1652278538.516289",
    :parent_user_id "U01G7GP6L5B",
    :team "T01G3T4BVGE",
    :client_msg_id "507b9035-86a8-4518-ac76-0555e638b09a",
    :blocks
    [{:type "rich_text",
      :block_id "OBl",
      :elements
      [{:type "rich_text_section",
        :elements [{:type "text", :text "reply to this thread"}]}]}],
    :user "U01G7GP6L5B",
    :text "reply to this thread"}])

(map (juxt :type :subtype keys) msgs)

[["message" nil
  [:channel_type :event_ts :channel :type :ts :team :client_msg_id :blocks :user :text]]
 ["message" "message_changed"
  [:channel_type :event_ts :channel :type :previous_message :ts :hidden :message :subtype]]
 ["message" "message_deleted"
  [:channel_type :event_ts :channel :type :previous_message :ts :hidden :deleted_ts :subtype]]
 ["reaction_added" nil [:type :user :reaction :item :item_user :event_ts]]
 ["reaction_removed" nil [:type :user :reaction :item :item_user :event_ts]]
 ["message"
  nil
  [:channel_type :event_ts :channel :type :thread_ts :ts :parent_user_id :team :client_msg_id :blocks :user :text]]]
