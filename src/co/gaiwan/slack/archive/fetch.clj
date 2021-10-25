(ns co.gaiwan.slack.archive.fetch
  (:require [clojure.java.io :as io]
            [co.gaiwan.slack.time-util :as time-util]
            [co.gaiwan.slack.api :as slack-api]
            [co.gaiwan.json-lines :as jsonl])
  (:import (java.io File)))

(defn fetch-users [arch token]
  (jsonl/spit-jsonl (io/file (:dir arch) "users.jsonl") (slack-api/users (slack-api/conn token))))

(defn fetch-channels [arch token]
  (jsonl/spit-jsonl (io/file (:dir arch) "channels.jsonl") (slack-api/conversations (slack-api/conn token))))

(defn fetch-emoji [arch token]
  (jsonl/spit-jsonl (io/file (:dir arch) "emoji.jsonl") (slack-api/emoji (slack-api/conn token))))

(fetch-emoji {:dir "/tmp"} token)

(do
  (time (count
         (jsonl/slurp-keywordized "/tmp/channels.jsonl")))
  (time (count
         (jsonl/slurp-jsonl "/tmp/channels.jsonl"))))
