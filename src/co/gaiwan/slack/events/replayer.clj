(ns co.gaiwan.slack.events.replayer)

;; We have an EventSource protocol that allows you to register
;; listeners, which then receive raw slack events. Currently there is
;; one implementation, provided by the slack socket API.

#_(defprotocol EventSource
    (add-listener [this watch-key listener]
      "Register a listener (single-arity function) which will receive raw slack events.
    `watch-key` functions as with [[add-watch]]: passing the same value again
    will replace the previous listener for that key")
    (remove-listener [this watch-key]
      "Remove a previously added listener"))

;; The plan is to make a second implementation which takes a
;; `json-lines` file, and (re-)emits the events in that file. This is
;; mainly meant for development, to have a way to feed fake/fixture
;; data into another component to see how it behaves.

;; API

(defprotocol EventSource
  (add-listener [this watch-key listener])
  (remove-listener [this watch-key]))

(defrecord ReplayerEventSource
    [sorted-events                      ; Atom of sorted sequence events.
     listeners                          ; Atom of listeners.
     offset-ms
     speed]

  EventSource
  (add-listener [this watch-key listener]
    (swap! listeners assoc watch-key listener))
  (remove-lintener [this watch-key]
    (swap! listeners dissoc watch-key)))

;; - `(from-events collection-of-raw-events {:as opts :keys [offset-ms speed] :or {speed 1}})`
;;     - take a collection of raw slack events (a sequence/vector/set containing maps)
;;     - returns a EventSource (see the protocols namespace)
;;         - You can refer to socket-api for some ideas around how to handle listeners
;;         - in socket-api we reified this protocol, in this case maybe it’s better to make it a record, `ReplayerEventSource`
;;             - Fields
;;                 - atom of sorted sequence of events
;;                 - atom of listeners (could be one state atom too)
;;                 - offset-ms / speed
;;     - configuration: `offset-ms`
;;         - if this is omitted, then it should offset such that the first event (by timestamp) in the collection is emitted immediately, and the ones after that relative to the first
;;     - `speed`: a factor for how quickly time passes, useful for debugging/dev to speed up the replay

;; - `(from-json json-file opts)`
;;     - read in the json-lines file and pass it on to `(from-events ...)`, opts as above

(defn from-events [raw-events {:keys [offset-ms speed]
                               :as opts
                               :or {speed 1}}]
  ;; raw-events is a sequence/vector/set containing maps.
  (map->ReplayerEventSource {:sorted-events atom-of-sorted-sequence-of-events
                             :listeners atom-of-listeners
                             :offset-ms offset-ms
                             :speed speed})

  ;; return EventSource
  ;;     - configuration: `offset-ms`
  ;;         - if this is omitted, then it should offset such that the first event (by timestamp) in the collection is emitted immediately, and the ones after that relative to the first
  ;;     - `speed`: a factor for how quickly time passes, useful for debugging/dev to speed up the replay
  )

(defn from-json [json-file opts])
;; read in the json-lines file and pass it on to `(from-events ...)`, opts as above
