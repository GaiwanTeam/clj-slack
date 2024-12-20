(ns co.gaiwan.slack.events.replayer
  "EventSource that replays pre-recorded Slack events."
  (:require
   [clojure.string :as str]
   [clojure.walk :as walk]
   [co.gaiwan.json-lines :as jsonl]
   [co.gaiwan.slack.events.util :as util]
   [co.gaiwan.slack.protocols :as protocols]
   [co.gaiwan.slack.raw-event :as raw-event]
   [co.gaiwan.slack.time-util :as time-util])
  (:import
   (java.time ZoneOffset)))

(defrecord ReplayerEventSource
    [sorted-events                      ; Atom. Sorted sequence of events.
     listeners                          ; Atom. Map of listeners.
     offset-us
     speed]

  protocols/EventSource
  (add-listener [this watch-key listener]
    (swap! listeners assoc watch-key listener))
  (remove-listener [this watch-key]
    (swap! listeners dissoc watch-key))

  java.io.Closeable
  (close [this]
    (reset! sorted-events [])))

;; Timestamps on the Slack API:
;; The timestamps used by the Slack API are strings representing a
;; [UNIX time](https://en.wikipedia.org/wiki/Unix_time), with microseconds
;; after the decimal point.

;; Contextual definitions:
;; s :: seconds
;; us :: microseconds
;; ts :: Slack’s API timestamp (e.g. "1614822402.022400")
;; us-ts :: timestamp as a Long in microseconds

(defn now-micros []
  (long (* 1e3 (System/currentTimeMillis))))

(defn offset-event
  "Returns `event` with `offset-us` applied to its timestamps."
  [event offset-us]
  (walk/postwalk
   (fn [form]
     (if-not (string? form)
       form
       (if-let [micros (time-util/ts->micros form)]
         (util/micros->ts (+ micros offset-us))
         form)))
   event))

(defn adjusted-event-time
  "Returns time as if `event` happened now shifted by `offset-us` and `speed`,
  assuming replaying started at `start-time`. All times are in microseconds."
  [start-time offset-us speed event]
  ;; take the time the event actually happened
  (-> (util/event-ts-micros event)
      ;; shift it to the new timeline, based on the given offset
      (+ offset-us)
      ;; take interval between when replay started, and when the event would normally fire (at speed=1)
      (- start-time)
      ;; shrink the elapsed interval based on the speed
      (/ speed)
      ;; turn interval back into a timestamp
      (+ start-time)))

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
               :or   {listeners {} speed 1}}]
  {:pre [(every? map? raw-events)]}
  (let [sorted-events (util/into-event-set raw-events)
        offset-us (or offset-us
                      (- (now-micros)
                         (util/event-ts-micros (first sorted-events))))]
    (map->ReplayerEventSource {:sorted-events (atom sorted-events)
                               :listeners     (atom listeners)
                               :offset-us     offset-us
                               :speed         speed})))

(defn from-json
  "Reads raw events from `jsonl-file`. Passes to [[from-events]].

  Check out [[from-events]] for details on supported `opts`.

  Refer to [[co.gaiwan.json-lines]] for more information on the JSON
  lines format and parsing."
  [jsonl-file {:keys [listeners offset-us speed] :as opts}]
  (let [raw-events (jsonl/slurp-jsonl jsonl-file)]
    (from-events raw-events opts)))

(defn replay!
  [{:keys [sorted-events listeners offset-us speed stop? done?]
    :or {stop? (volatile! false)}
    :as replayer}]
  (let [start-time (now-micros)
        adjust (partial adjusted-event-time start-time offset-us speed)]
    (loop [now start-time]
      (when-not @stop?
        (let [expired-events (take-while
                              (fn [e]
                                (< (adjust e) now))
                              @sorted-events)]

          (swap! sorted-events #(drop (count expired-events) %))

          (doseq [e expired-events
                  :let [e (offset-event e offset-us)]]
            (run! (fn [l] (l e)) (vals @listeners)))

          (when-let [remaining (seq @sorted-events)]
            (Thread/sleep (max (/ (- (adjust (first remaining)) now)
                                  1000)
                               50))

            (recur (now-micros))))))))

(defn start!
  "Kick of the replay in a separate thread (future). Returns a replayer with
  additional keys `:done?` (deref to wait for completion) and `:stop!` (call as
  a function to interrupt)."
  [replayer]
  (let [stop? (volatile! false)
        done? (future (replay! (assoc replayer :stop? stop?)))]
    (assoc replayer :done? done? :stop! #(vreset! stop? true))))
#_
(defn replay!
  [{:keys [sorted-events listeners offset-us speed] :as replayer}]
  (let [start-time (now-micros)
        adjust (partial adjusted-event-time start-time offset-us speed)]
    (loop [now start-time]
      (let [expired-events ...] ;;FIXME

        ;; FIXME
        ;; remove expired events
        ;; emit expired events

        (when-let [remaining (seq @sorted-events)]
          (Thread/sleep ,,,) ;; FIXME
          (recur (now-micros)))))))

(defn seek!
  "Seek the replayer to a time, given as a 'HH:MM' string. Use this before
  starting the replayer."
  [replayer time]
  (let [event (first @(:sorted-events replayer))
        inst (time-util/ts->inst (raw-event/event-ts event))
        [hh mm] (map #(Long/parseLong %)
                     (str/split time #":"))
        ts (time-util/inst->ts
            (.. inst
                (atZone ZoneOffset/UTC)
                (withHour hh)
                (withMinute mm)
                toInstant
                ))]
    (swap! (:sorted-events replayer)
           (fn [es]
             (doall
              (drop-while (fn [event]
                            (< (compare (raw-event/event-ts event) ts) 0))
                          es))))
    (assoc replayer :offset-us
           (- (now-micros)
              (time-util/ts->micros ts)))))

(comment

  (def rrr
    (from-json "/home/arne/ITRevolution/devopsenterprise-slack-archive/logs/2022-05-11.txt"
               {}))
  (compare   "1652230067.075500" "1652263247.075500")
  (compare 1 2)
  (def rr2 (seek! rrr "10:00"))
  (time-util/ts->inst
   (raw-event/event-ts
    (first @(:sorted-events rrr))))
  (time-util/ts->inst
   (raw-event/event-ts
    (first @(:sorted-events rr2))))

  [(:offset-us rrr)
   (:offset-us rr2)
   (double
    (/ (-
        (:offset-us rrr)
        (:offset-us rr2))
       1e6
       60
       60))]

  (replay! (from-events [{"ts" "0000000001.000000"}
                         {"ts" "0000000003.000000"}
                         {"ts" "0000000006.000000"}
                         {"ts" "0000000009.000000"}
                         {"ts" "0000000012.000000"}]
                        {:listeners {::prn prn}})))
