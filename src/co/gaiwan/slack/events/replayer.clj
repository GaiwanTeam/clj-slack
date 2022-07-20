(ns co.gaiwan.slack.events.replayer
  "EventSource that replays pre-recorded Slack events."
  (:require [co.gaiwan.slack.protocols :as protocols]))

(defrecord ReplayerEventSource
    [sorted-events                      ; Atom of sorted sequence events.
     listeners                          ; Atom of listeners.
     offset-us
     speed]

  protocols/EventSource
  (add-listener [this watch-key listener]
    (swap! listeners assoc watch-key listener))
  (remove-listener [this watch-key]
    (swap! listeners dissoc watch-key)))

(defn offset-event
  "Take a raw slack event, find all timestamps, and change them by adding offset-us, in microseconds."
  [event offset-us]
  ;; use clojure.walk/postwalk to transform the event map
  ;; find each item that looks like a timestamp, e.g. "1614822402.022400"
  ;; remove the dot and parse as integer (Long/parseLong ...)
  ;; add offset-ms
  ;; split into seconds and microseconds (seconds = divide by 1e6 and round down ; microseconds = modulo 1e6)
  ;; turn back into a string
  )

(defn from-events
  "Take a collection of raw slack events (a sequence/vector/set containing maps)
  and turn it into a [[protocols/EventSource]]

  Options:
  - `offset-us`: the number of microseconds that should be added to each event timestamp.
  If this is omitted, then an offset will be calculated such that the first event (by timestamp) in the collection is emitted immediately (at the current time), and the ones after that relative to the first
  - `speed`: a factor for how quickly time passes. Defaults to `1` (normal speed). Useful for debugging/dev to speed up the replay.
  - `listeners`: initial map of listeners (map of keyword to function)."
  [raw-events {:keys [offset-us speed listeners]
               :as opts
               :or {speed 1
                    listeners {}}}]
  ;; raw-events is a sequence/vector/set containing maps.
  (map->ReplayerEventSource {:sorted-events (atom nil)
                             :listeners (atom listeners)
                             :offset-us offset-us
                             :speed speed}))

;; read in the json-lines file and pass it on to `(from-events ...)`, opts as above
(defn from-json [json-file opts])

(comment
  (require '[co.gaiwan.slack.test-data.raw-events :as test-events])

  (def replayer
    (from-events test-events/replies+broadcast {:listeners
                                                {::prn prn}}))


  (offset-event {"event_ts" "1614852449.028400"
                 "ts" "1614852449.028400"
                 "user" "U061V0GG2"
                 "client_msg_id" "beacf6fd-b6f0-4ebf-b313-664b16f0f576"
                 "text" "Hmm in Compojure-api/Ring-swagger this was using describe/field function but it is a bit different here."
                 "suppress_notification" false
                 "thread_ts" "1614822402.022400"
                 "source_team" "T03RZGPFR"
                 "type" "message"
                 "channel" "C7YF1SBT3"
                 "team" "T03RZGPFR"
                 "user_team" "T03RZGPFR"}
                123456789 ;; 123.456789 seconds
                )
  )
