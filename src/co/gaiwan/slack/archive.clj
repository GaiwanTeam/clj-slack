(ns co.gaiwan.slack.archive
  "Working with an archive as a set of files, partitioned by channel and day.

  A Archive in this sense is a directory containing per-channel folders, which
  contain per-day json-lines files, with Slack events pre-filtered and
  partitioned, so everything related to a given day is available in one file. It
  also contains top-level jsonl files for the slack workspace's users, channels,
  and emoji

  ```
  ├── users.jsonl
  ├── channels.jsonl
  ├── emoji.jsonl
  ├── GQ54T7CCX
  │   └── 2020-06-16.jsonl
  └── GQ6JA3UBF
      ├── 2019-11-05.jsonl
      ├── 2020-05-29.jsonl
      └── 2020-06-23.jsonl
  ```

  When working with such an archive we have a `{:dir \"archive-directory\"}` map
  called arch, which may also contain other things to help incrementally
  construct the archive."
  (:require [clojure.java.io :as io]
            [co.gaiwan.json-lines :as jsonl]
            [co.gaiwan.slack.normalize.events-api :as normalize]
            [co.gaiwan.slack.archive.api-resources :as api-resources]
            [co.gaiwan.slack.archive.partition :as partition]
            [co.gaiwan.slack.normalize.web-api :as norm-web]
            [co.gaiwan.slack.raw-archive :as raw-archive]))

(defn archive
  "Create a new map identifying the archive.

  This value is passed through operations which may add extra stuff to it."
  [root-dir]
  {:dir root-dir})

(defn slurp-chan-day-raw
  "Get the raw list of events for a given channel and day"
  [arch channel-id day-str]
  (let [f (partition/chan-day-file (:dir arch) channel-id day-str)]
    (when (.exists f)
      (jsonl/slurp-jsonl f))))

(defn slurp-chan-day
  [arch channel-id day-str]
  (normalize/message-data (slurp-chan-day-raw arch channel-id day-str)))

(defn load-api-resources
  "Load the channel, user, and emoji data stored at the top level of the archive
  in json-lines files, see [[fetch-api-resources]]."
  [arch]
  (-> arch
      api-resources/load-channels
      api-resources/load-users
      #_api-resources/load-emoji))

(defn default-event-filter-predicate
  "The default predicate used by [[build-archive]] to
  determine whether an event should get added to the archive or not.

  - Only consider regular channels (id starts with 'C'), not DMs or group messages
  - Only consider message and reaction events, and for message events only a
    specific set of subtypes."
  [{:strs [type subtype channel]}]
  (and (string? channel)
       (= \C (first channel))
       (or (#{"reaction_added" "reaction_removed"} type)
           (and (= "message" type)
                (contains? #{nil
                             "message_changed"
                             "message_replied"
                             "thread_broadcast"
                             "message_deleted"
                             "tombstone"
                             "channel_archive"
                             "channel_join"
                             "channel_name"
                             "channel_purpose"
                             "channel_topic"
                             "me_message"
                             "reminder_add"
                             "slack_image"
                             "bot_message"}
                           subtype)))))

(defn raw->archive
  "Build up a partitioned archive from a raw archive, by providing the source and
  destination directory."
  ([raw-dir archive-dir]
   (build-archive raw-dir archive-dir nil))
  ([raw-dir archive-dir {:keys [exts filter-by]
                         :or {exts [".txt" ".jsonl"]
                              filter-by default-event-filter-predicate}}]
   (partition/into-archive
    archive-dir
    (filter filter-by)
    (raw-archive/dir-event-seq raw-dir exts))))

(defn fetch-api-resources
  "Retrieve channel, user, and emoji data via the Slack web API, and store them in
  json-lines files at the top level of the archive."
  [arch slack-token]
  (-> arch
      (api-resources/fetch-channels slack-token)
      (api-resources/fetch-users slack-token)
      (api-resources/fetch-emoji slack-token)))
