(ns co.gaiwan.slack.archive.api-resources
  "Handle the resources within a partitioned archive which are retrieved via the
  web API: users, channels, emoji."
  (:require [charred.api :as charred]
            [clojure.java.io :as io]
            [co.gaiwan.json-lines :as jsonl]
            [co.gaiwan.slack.api :as slack-api]
            [co.gaiwan.slack.domain.channel :as domain-channel]
            [co.gaiwan.slack.domain.user :as domain-user]))

(defn fetch-users
  "Fetch users via the web API, saving them to a `users.jsonl` json-lines file at
  the top level directory of the archive. Takes an archive map with a `:dir`
  key, and a Slack API token."
  [arch token]
  (let [users (slack-api/users (slack-api/conn token))]
    (jsonl/spit-jsonl (io/file (:dir arch) "users.jsonl")
                      users)
    (assoc arch :users users)))

(defn fetch-channels
  "Fetch channels via the web API, saving them to a `channels.jsonl` json-lines
  file at the top level directory of the archive. Takes an archive map with a
  `:dir` key, and a Slack API token."
  [arch token]
  (let [channels (slack-api/conversations (slack-api/conn token))]
    (jsonl/spit-jsonl (io/file (:dir arch) "channels.jsonl") channels)
    (assoc arch :channels channels)))

(defn fetch-emoji
  "Fetch emoji via the web API, saving them to a `emoji.jsonl` json-lines file at
  the top level directory of the archive. Takes an archive map with a `:dir`
  key, and a Slack API token."
  [arch token]
  (let [emoji (into {} (slack-api/emoji (slack-api/conn token)))]
    (charred/write-json (io/file (:dir arch) "emoji.json")
                        (into {}
                              (slack-api/emoji (slack-api/conn token))))
    (assoc arch :emoji emoji)))

(defn load-users
  "Load the raw user data from the `users.jsonl` file, normalize them, and add the
  normalized data under `:users` in the archive map."
  [{:keys [dir] :as arch}]
  (let [user-file (io/file dir "users.jsonl")]
    (if (.exists user-file)
      (let [users (jsonl/slurp-jsonl user-file)]
        (assoc arch
               :users (into {}
                            (comp (map domain-user/raw->user)
                                  (map (juxt :user/id identity)))
                            users)))
      arch)))

(defn load-channels
  "Load the raw channel data from the `channels.jsonl` file, normalize them, and
  add the normalized data under `:channels` in the archive map."
  [{:keys [dir] :as archive}]
  (let [channel-file (io/file dir "channels.jsonl")]
    (if (.exists channel-file)
      (assoc archive :channels (into {}
                                     (comp (map domain-channel/raw->channel)
                                           (map (juxt :channel/id identity)))
                                     (jsonl/slurp-jsonl channel-file)))
      archive)))

;; Fixme : these array-typed entries are not cooperating, use maps instead
(defn load-emoji
  "Load the raw custom emoji data from the `emoji.jsonl` file, and add the
  normalized data under `:emoji` in the archive map."
  [{:keys [dir] :as archive}]
  (let [emoji-file (io/file dir "emoji.json")]
    (if (.exists emoji-file)
      (assoc archive :emoji (charred/read-json emoji-file))
      archive)))

;; (into {})
;; (frequencies
;;  (map count
;;       (jsonl/slurp-jsonl "/tmp/cljians-archive/emoji.jsonl" )))
