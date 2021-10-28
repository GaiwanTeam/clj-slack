(ns co.gaiwan.slack.archive.api-resources
  "Handle the resources within a partitioned archive which are retrieved via the
  web API: users, channels, emoji."
  (:require [clojure.java.io :as io]
            [co.gaiwan.json-lines :as jsonl]
            [co.gaiwan.slack.api :as slack-api]
            [co.gaiwan.slack.normalize.web-api :as norm-web]))

(defn fetch-users
  "Fetch users via the web API, saving them to a `users.jsonl` json-lines file at
  the top level directory of the archive. Takes an archive map with a `:dir`
  key, and a Slack API token."
  [arch token]
  (jsonl/spit-jsonl (io/file (:dir arch) "users.jsonl") (slack-api/users (slack-api/conn token)))
  arch)

(defn fetch-channels
  "Fetch channels via the web API, saving them to a `channels.jsonl` json-lines
  file at the top level directory of the archive. Takes an archive map with a
  `:dir` key, and a Slack API token."
  [arch token]
  (jsonl/spit-jsonl (io/file (:dir arch) "channels.jsonl") (slack-api/conversations (slack-api/conn token)))
  arch)

(defn fetch-emoji
  "Fetch emoji via the web API, saving them to a `emoji.jsonl` json-lines file at
  the top level directory of the archive. Takes an archive map with a `:dir`
  key, and a Slack API token."[arch token]
  (jsonl/spit-jsonl (io/file (:dir arch) "emoji.jsonl") (slack-api/emoji (slack-api/conn token)))
  arch)

(defn load-users
  "Load the raw user data from the `users.jsonl` file, normalize them, and add the
  normalized data under `:users` in the archive map."
  [{:keys [dir] :as arch}]
  (let [user-file (io/file dir "users.jsonl")]
    (if (.exists user-file)
      (let [users (jsonl/slurp-jsonl user-file)]
        (assoc arch
               :users (into {}
                            (comp (map norm-web/user+profile)
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
                                     (comp (map norm-web/channel)
                                           (map (juxt :channel/id identity)))
                                     (jsonl/slurp-jsonl channel-file)))
      archive)))

;; Fixme : these array-typed entries are not cooperating, use maps instead
(defn load-emoji
  "Load the raw custom emoji data from the `emoji.jsonl` file, and add the
  normalized data under `:emoji` in the archive map."
  [{:keys [dir] :as archive}]
  (let [emoji-file (io/file dir "emoji.jsonl")]
    (if (.exists emoji-file)
      (assoc archive :emoji (into {}
                                  (jsonl/slurp-jsonl emoji-file jsonl/jsonista-mapper java.util.List)))
      archive)))
