(ns co.gaiwan.slack.test-data.entities)

(def user-map
  {"U4F2A0Z8ER" "John"
   "U4F2A0Z9HR" "Marry"})

(def users-normalized
  [{:user-profile/display-name-normalized "Slackbot",
    :user/owner? false,
    :user/name "slackbot",
    :user/id "USLACKBOT",
    :user-profile/real-name-normalized "Slackbot",
    :user-profile/avatar-hash "sv41d8cd98f0",
    :user-profile/email nil,
    :user-profile/image-32 "https://a.slack-edge.com/80588/img/slackbot_32.png",
    :user-profile/image-48 "https://a.slack-edge.com/80588/img/slackbot_48.png",
    :user-profile/display-name "Slackbot",
    :user-profile/image-72 "https://a.slack-edge.com/80588/img/slackbot_72.png",
    :user-profile/image-192
    "https://a.slack-edge.com/80588/marketing/img/avatars/slackbot/avatar-slackbot.png",
    :user/real-name "Slackbot",
    :user-profile/image-512
    "https://a.slack-edge.com/80588/img/slackbot_512.png",
    :user-profile/real-name "Slackbot",
    :user/admin? false,
    :user-profile/image-24 "https://a.slack-edge.com/80588/img/slackbot_24.png"}

   {:user-profile/display-name-normalized "mitesh",
    :user/owner? true,
    :user/name "mitesh",
    :user/id "U01FVSUGVN3",
    :user-profile/real-name-normalized "Mitesh (oxalorg)",
    :user-profile/avatar-hash "0f1d70de038c",
    :user-profile/email nil,
    :user-profile/image-32 "https://avatars.slack-edge.com/2021-01-11/1626319808851_0f1d70de038cff2f61ec_32.jpg",
    :user-profile/image-48 "https://avatars.slack-edge.com/2021-01-11/1626319808851_0f1d70de038cff2f61ec_48.jpg",
    :user-profile/display-name "mitesh",
    :user-profile/image-72 "https://avatars.slack-edge.com/2021-01-11/1626319808851_0f1d70de038cff2f61ec_72.jpg",
    :user-profile/image-192 "https://avatars.slack-edge.com/2021-01-11/1626319808851_0f1d70de038cff2f61ec_192.jpg",
    :user/real-name "Mitesh (oxalorg)",
    :user-profile/image-512 "https://avatars.slack-edge.com/2021-01-11/1626319808851_0f1d70de038cff2f61ec_512.jpg",
    :user-profile/real-name "Mitesh (oxalorg)",
    :user/admin? true,
    :user-profile/image-24 "https://avatars.slack-edge.com/2021-01-11/1626319808851_0f1d70de038cff2f61ec_24.jpg"}

   {:user-profile/display-name-normalized "Arne Brasseur",
    :user/owner? true,
    :user/name "arne",
    :user/id "U01G7GP6L5B",
    :user-profile/real-name-normalized "Arne Brasseur",
    :user-profile/avatar-hash "d7b9434a78f1",
    :user-profile/email nil,
    :user-profile/image-32 "https://avatars.slack-edge.com/2020-12-07/1548912027190_d7b9434a78f197884eb7_32.jpg",
    :user-profile/image-48 "https://avatars.slack-edge.com/2020-12-07/1548912027190_d7b9434a78f197884eb7_48.jpg",
    :user-profile/display-name "Arne Brasseur",
    :user-profile/image-72 "https://avatars.slack-edge.com/2020-12-07/1548912027190_d7b9434a78f197884eb7_72.jpg",
    :user-profile/image-192 "https://avatars.slack-edge.com/2020-12-07/1548912027190_d7b9434a78f197884eb7_192.jpg",
    :user/real-name "Arne Brasseur",
    :user-profile/image-512 "https://avatars.slack-edge.com/2020-12-07/1548912027190_d7b9434a78f197884eb7_512.jpg",
    :user-profile/real-name "Arne Brasseur",
    :user/admin? true,
    :user-profile/image-24 "https://avatars.slack-edge.com/2020-12-07/1548912027190_d7b9434a78f197884eb7_24.jpg"}])
