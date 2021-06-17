(ns co.gaiwan.slack.schema
  (:require [co.gaiwan.data-tools.schema-derivations :as schema-derivations]
            [malli.core :as malli]
            [malli.error :as malli-error]
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

(defn validate! [type value]
  (when-not (malli/validate type value {:registry malli-registry})
    (throw (ex-info (str "Invalid " type)
                    (-> (malli/explain type value {:registry malli-registry})
                        (malli-error/humanize))))))

(comment
  (schema-derivations/datomic-schema schema))
