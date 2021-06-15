(ns co.gaiwan.slack.normalize.web-api
  "Code for converting entities as we get them from the Slack web API into our own
  data format.")

(defn user [{:keys [id name real_name is_admin is_owner profile]}]
  {:slack.user/slack-id id
   :slack.user/name name
   })

(defn user-profile [{:keys [image_512 email first_name real_name_normalized
                            image_48 image_192 real_name image_72 image_24
                            avatar_hash team image_32 last_name display_name
                            display_name_normalized]}]
  {})

#_
(defn user->tx [{:keys [id name real_name is_admin is_owner profile]}]
  (let [{:keys [image_512 email first_name real_name_normalized image_48 image_192
                real_name image_72 image_24 avatar_hash team image_32 last_name
                display_name display_name_normalized]} profile]
    (->> (merge #:user {:slack-id id
                        :name name
                        :real-name real_name
                        :admin? is_admin
                        :owner? is_owner}
                #:user-profile {:email email,
                                :avatar-hash avatar_hash,
                                :image-32 image_32,
                                :image-24 image_24,
                                :image-192 image_192,
                                :image-48 image_48,
                                :real-name-normalized real_name_normalized,
                                :display-name-normalized display_name_normalized,
                                :display-name display_name,
                                :image-72 image_72,
                                :real-name real_name,
                                :image-512 image_512})
         (remove (comp nil? val))
         (into {}))))
