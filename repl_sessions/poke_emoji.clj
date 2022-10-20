(ns repl-sessions.poke-emoji
  (:require [co.gaiwan.slack.markdown :as md]))

(sort (set (mapcat identity (keys @md/standard-emoji-map))))
