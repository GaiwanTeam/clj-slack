(ns co.gaiwan.slack.domain.user
  "Schema and logic for manipulating User data")

(def ?User
  [:map
   [:user/id string?]
   [:user/name string?]
   [:user/real-name string?]
   [:user/admin? boolean?]
   [:user/owner? boolean?]
   [:user-profile/display-name-normalized string?]
   [:user-profile/real-name-normalized string?]
   [:user-profile/avatar-hash string?]
   [:user-profile/email [:maybe string?]]
   [:user-profile/image-32 string?]
   [:user-profile/image-48 string?]
   [:user-profile/display-name string?]
   [:user-profile/image-72 string?]
   [:user-profile/image-192 string?]
   [:user-profile/image-512 string?]
   [:user-profile/real-name string?]
   [:user-profile/image-24 string?]])

(defn raw->user [raw-user]
  (let [{:strs [id name real_name is_admin is_owner profile]} raw-user
        {:strs [image_512 email first_name real_name_normalized image_48 image_192
                image_72 image_24 avatar_hash team image_32 last_name
                display_name display_name_normalized]} profile]
    {:user/id id
     :user/name name
     :user/real-name real_name
     :user/admin? is_admin
     :user/owner? is_owner

     :user-profile/display-name-normalized display_name_normalized
     :user-profile/real-name-normalized real_name_normalized
     :user-profile/avatar-hash avatar_hash
     :user-profile/email email
     :user-profile/image-32 image_32
     :user-profile/image-48 image_48
     :user-profile/display-name display_name
     :user-profile/image-72 image_72
     :user-profile/image-192 image_192
     :user-profile/image-512 image_512
     :user-profile/real-name (get profile "real_name")
     :user-profile/image-24 image_24}))
