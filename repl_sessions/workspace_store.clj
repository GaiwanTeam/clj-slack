 (ns repl-sessions.workspace_store
   (:require [co.gaiwan.slack.test-data.raw-events :as raw-events]
             [co.gaiwan.slack.raw-event :as raw-event]
            [co.gaiwan.slack.test-data.entities :as entities]
            [co.gaiwan.slack.domain.workspace-store :as workspace-store-schema]))

  (def store 
    (atom 
     (workspace-store-schema/new-store 
      {:workspace         "gaiwan-team"
       :markdown-handlers {}})))

  (swap! store workspace-store-schema/add-users entities/users-normalized)

  (let [events     raw-events/single-reply
        channel-id (raw-event/channel-id (first events))]
    (swap! store workspace-store-schema/add-channel-events channel-id events))
  
  (swap! store workspace-store-schema/enrich-entries ["1550831541.063800"])
  
  (workspace-store-schema/add-raw-event store raw-events/single-new-msg)
  (workspace-store-schema/add-raw-event store raw-events/single-reply2) ;; => ERROR, not finding the affected althogh there
