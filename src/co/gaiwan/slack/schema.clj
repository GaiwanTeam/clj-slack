(ns co.gaiwan.slack.schema
  (:require [co.gaiwan.data-tools.schema-derivations :as schema-derivations]
            [malli.core :as malli]
            [malli.registry :as malli-reg]))

(def schema
  {"slack"
   {:doc ""
    :entities
    {"message"
     {:primary-key "timestamp"
      :attributes
      {"timestamp" {:data-type :string}
       "text" {:data-type :string}
       "team" {:data-type :foreign-key
               :references ["chat" "team"]}
       "channel" {:data-type :foreign-key
                  :references ["chat" "channel"]}}}
     "channel"
     {:attributes
      {}}
     "user"
     {:primary-key "id"
      :attributes
      {"id" {:data-type :string}
       "name" {:data-type :string}
       "avatar-image-url" {:data-type :string}}}
     "team"
     {:attributes
      {}}}}})

(def malli-registry
  (malli-reg/registry
   (merge (malli-reg/-schemas malli/default-registry)
          (schema-derivations/malli-registry schema {:closed? true}))))

(comment
  (schema-derivations/datomic-schema schema))
