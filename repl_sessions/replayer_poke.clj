(ns repl-sessions.replayer-poke
  (:require
   [co.gaiwan.slack.events.replayer :as replayer]
   [co.gaiwan.slack.events.util :as events-util]))

(def replayer
  (replayer/start!
   (replayer/from-json
    "/home/arne/github/clojurians-log-demo-data/logs/2018-02-01.txt"
    #_    "/home/arne/github/clojurians-log/logs/2021-06-12.txt"
    {:listeners {::prn #(println (events-util/format-evt % 90))}
     :speed 20})))

;; ask it to stop, not instantaneous
((:stop! replayer))

;; actually done
@(:done? replayer)
