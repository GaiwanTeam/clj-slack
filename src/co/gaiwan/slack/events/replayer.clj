(ns co.gaiwan.slack.events.replayer
  "EventSource that replays pre-recorded Slack events."
  (:require [clojure.math :as math]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [co.gaiwan.json-lines :as jsonl]
            [co.gaiwan.slack.protocols :as protocols])
  (:import (java.util.concurrent TimeUnit)))

(defrecord ReplayerEventSource
    [sorted-events                      ; Atom. Sorted sequence of events.
     listeners                          ; Atom. Map of listeners.
     offset-us
     speed]

  protocols/EventSource
  (add-listener [this watch-key listener]
    (swap! listeners assoc watch-key listener))
  (remove-listener [this watch-key]
    (swap! listeners dissoc watch-key)))

;;; Timestamps on the Slack API:
;;; The timestamps used by the Slack API are strings representing a
;;; [UNIX time](https://en.wikipedia.org/wiki/Unix_time), with microseconds
;;; after the decimal point.

;;; Contextual definitions:
;;; s :: seconds
;;; us :: microseconds
;;; ts :: Slack’s API timestamp (e.g. "1614822402.022400")
;;; us-ts :: timestamp as a Long in microseconds

(def ts-regex #"(\d{10})\.(\d{6})")

(defn ts->micros
  "If `s` is a ts string, converts it to Long. Else, returns nil."
  [s]
  (when-let [[_ seconds micros] (re-find ts-regex s)]
    (parse-long (str seconds micros))))

(defn micros->ts
  "Converts `micros` into a timestamp string."
  [micros]
  (->> micros
       (format "%016d")
       (re-find #"(\d{10})(\d{6})")
       rest
       (str/join \.)))

(defn micros->millis [micros]
  (.toMillis (TimeUnit/MICROSECONDS) micros))

#_
(defn current-time-micros
  "Returns the current time (at evaluation) in microseconds."
  []
  (.toMicros (TimeUnit/MILLISECONDS) (System/currentTimeMillis)))

(defn get-event-time
  "Derives `event` time (in microseconds) from its timestamp."
  [{:strs [ts] :as _event}]
  (ts->micros ts))

(defn sort-events-chronologically
  "Returns `events` sorted by reverse chronological order.
  `events` is a sequence, vector or set of raw events."
  [events]
  (sort-by #(get % "ts") events))

(defn offset-event
  "Returns `event` with `offset-us` applied to its timestamps."
  [event offset-us]
  (walk/postwalk
   (fn [form]
     (if-not (string? form)
       form
       (if-let [micros (ts->micros form)]
         (micros->ts (+ micros offset-us))
         form)))
   event))

(defn calc-offset
  "Returns time as if `event` happened now shifted by `offset-us` and `speed`."
  [base-time event offset-us speed]
  ;; Take the start time of event,
  (let [start-time (get-event-time event)]
    ;; calculate the elapsed time since it started,
    (-> (- start-time base-time)
        ;; control the playback speed,
        (* speed)
        ;; bring forward in time,
        (+ base-time)
        ;; and adjust the offset.
        (+ offset-us))))

(defn offset-events
  "Shifts `sorted-events` timestamps by `offset-us` and `speed`"
  [base-time sorted-events offset-us speed]
  (let [now (atom base-time)]
    (map (fn [event]
           (let [offset (calc-offset @now event offset-us speed)]
             (offset-event event offset)
             (reset! now offset))) sorted-events)))

(defn from-events
  "Turns `raw-events` into an [[protocols/EventSource]].

  `raw-events` can be a sequence, vector or set containing raw event
  maps. Events will get sorted chronologically by timestamp.

  Supported options in `opts`:

  | Key          | Description                                                         |
  |:-------------|:--------------------------------------------------------------------|
  | `:listeners` | Initial map of listeners (map of keyword to function)               |
  | `:offset-us` | Number of microseconds that should be added to each event timestamp |
  | `:speed`     | A factor for how quickly time passes. Defaults to 1 (normal speed). |

  Note: If `:offset-us` is omitted, [[replay!]] calculates an offset
  such that the first event in the sorted collection is emitted
  immediately (at the current time), and the ones after that relative
  to the first."
  [raw-events {:keys [listeners offset-us speed] :as _opts
               :or   {listeners {}, offset-us 0, speed 1}}]
  {:pre [(every? map? raw-events)]}
  (let [sorted-events (sort-events-chronologically raw-events)]
    (map->ReplayerEventSource {:sorted-events (atom sorted-events)
                               :listeners     (atom listeners)
                               :offset-us     offset-us
                               :speed         speed})))

(defn from-json
  "Reads raw events from `jsonl-file`. Passes to [[from-events]].

  Check out [[from-events]] for details on supported `opts`.

  Refer to [[co.gaiwan.json-lines]] for more information on the JSON
  lines format and parsing."
  [jsonl-file {:keys [_listeners _offset-us _speed] :as opts}]
  (let [raw-events (jsonl/slurp-jsonl jsonl-file)]
    (from-events raw-events opts)))

(defn replay!
  [{:keys [sorted-events listeners offset-us speed]}]
  (let [start  (atom (get-event-time (first @sorted-events)))
        offset (atom offset-us)]
    (doseq [event @sorted-events
            :let  [event-time (get-event-time event)
                   interval (- event-time @start)
                   sped-up-interval interval ;TODO
                   offset-value @offset]]
      (cond
        (neg? offset-value)
        (do (prn "neg")
            (swap! offset + sped-up-interval)
            (run! #(% event) (vals @listeners))
            (reset! start event-time))

        (pos? offset-value)
        (do (prn "pos offset")
            (reset! offset 0)
            (Thread/sleep (micros->millis offset-value))

            (prn "pos event")
            (run! #(% event) (vals @listeners))
            (reset! start event-time))

        :else
        (do (prn "else")
            (Thread/sleep (micros->millis sped-up-interval))
            (run! #(% event) (vals @listeners))
            (reset! start event-time))))))

(comment
  (def replayer-test-easy
    {:sorted-events (atom [{"ts" "0000000001.000000"}
                           {"ts" "0000000003.000000"}
                           {"ts" "0000000006.000000"}
                           {"ts" "0000000009.000000"}
                           {"ts" "0000000012.000000"}])
     :listeners (atom {::prn prn})
     :offset-us -3000000
     :speed 1.5})

  (replay! replayer-test-easy))

#_(start-replay! replayer)

;; (future
;;   (start-replay!))

;; (def stop! (start-replay! (from events ,,,)))

(comment
  (require '[co.gaiwan.slack.test-data.raw-events :as test-events])

  (def replayer-test
    (from-events (rest test-events/replies+broadcast) {:offset-us 50000123
                                                  :listeners
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

  (defn make-ts []
    (->> #(rand-int 10N)
         repeatedly
         (take 16)
         (partition-all 10)
         (map str/join)
         (str/join \.)))



  )
