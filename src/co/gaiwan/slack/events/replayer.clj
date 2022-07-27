(ns co.gaiwan.slack.events.replayer
  "EventSource that replays pre-recorded Slack events."
  (:require [clojure.math :as math]
            [clojure.walk :as walk]
            [co.gaiwan.slack.protocols :as protocols]))

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

;;; Timestamps on the Slack API:
;;; The timestamps used by the Slack API are strings representing a
;;; [UNIX time](https://en.wikipedia.org/wiki/Unix_time), with microseconds
;;; after the decimal point.

;;; Contextual definitions:
;;; s :: second
;;; us :: microsecond
;;; ts :: Slack timestamp
;;; us-ts :: timestamp as a Long in microseconds

(def ts-regex #"(\d{10})\.(\d{6})")

(def ts? (comp boolean (partial re-matches ts-regex)))

(defn ts->us-ts
  "If ts is a Slack ts string, converts it to Long. Else returns nil."
  [ts]
  (when (string? ts)
    (when-let [[_ s us] (re-matches ts-regex ts)]
      (parse-long (str s us)))))

;; (defn ts->us-ts
;;   "Converts a Slack timestamp to a Long."
;;   [ts]
;;   (let [[s us] (str/split ts #"\.")]
;;     (parse-long (str s us))))

(defn left-pad
  ([s n]
   (left-pad s n \space))
  ([s n c]
   (if (>= (count s) n)
     s
     (left-pad (str c s) n c))))

(defn left-pad-0 [s n]
  (left-pad s n \0))

(defn us-ts->s [us-ts]
  (-> us-ts (/ 1e6) math/round str (left-pad-0 10)))

(defn us-ts->us [us-ts]
  (-> us-ts (mod 1e6) math/round str (left-pad-0 6)))

(defn us-ts->ts [us-ts]
  (let [s  (us-ts->s us-ts)
        us (us-ts->us us-ts)]
    (str s \. us)))

(defn offset-us-ts [us-ts offset-us]
  (+ us-ts offset-us))

(defn offset-event
  "Takes a raw Slack event and adds offset-us to all its timestamps."
  [event offset-us]
  (walk/postwalk
   (fn [form]
     (if-let [us-ts (ts->us-ts form)]
       (-> us-ts (offset-us-ts offset-us) us-ts->ts)
       form))
   event))

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


  (def make-ts
    (fn []
      (->> #(rand-int 9)
           repeatedly
           (take 16)
           (partition-all 10)
           (map (partial apply str))
           (clojure.string/join \.))))

  )
