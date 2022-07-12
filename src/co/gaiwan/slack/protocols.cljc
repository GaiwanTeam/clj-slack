(ns co.gaiwan.slack.protocols)

(defprotocol EventSource
  (add-listener [this watch-key listener]
    "Register a listener (single-arity function) which will receive raw slack events.
    `watch-key` functions as with [[add-watch]]: passing the same value again
    will replace the previous listener for that key")
  (remove-listener [this watch-key]
    "Remove a previously added listener"))
