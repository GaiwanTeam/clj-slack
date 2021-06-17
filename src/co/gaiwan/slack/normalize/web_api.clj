(ns co.gaiwan.slack.normalize.web-api
  "Code for converting entities as we get them from the Slack web API into our own
  data format.")

(defn normalize-user [{:keys [id name real_name is_admin is_owner profile]}]
  {:slack.user/id id
   :slack.user/name name})

(defn normalize-user-profile [{:keys [email team avatar_hash
                                      first_name last_name
                                      real_name real_name_normalized
                                      display_name  display_name_normalized
                                      image_24 image_32 image_48 image_72 image_192 image_512]}]
  {:slack.user/image-48 image_48})

(defn user->tx
  "Transform the raw slack user entity to datomic transaction data

  The relationship between datomic schema and raw slack user entity
  is defined by `normalize-user` and `normalize-user-profile`"
  ([user]
   (user->tx user normalize-user normalize-user-profile))
  ([{:keys [profile] :as user} normalize-user-f normalize-user-profile-f]
   (->> (merge (normalize-user-f user)
               (normalize-user-profile-f profile))
        (remove (comp nil? val))
        (into {}))))

(comment
  (def users-edn (read-string
                  (slurp "resources/co/gaiwan/slack/demo_users.edn")))
  (map user->tx users-edn))
