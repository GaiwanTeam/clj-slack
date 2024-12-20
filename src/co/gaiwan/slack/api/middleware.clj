(ns co.gaiwan.slack.api.middleware
  "Decorators for API request functions that handle cross cutting concerns"
  (:require [lambdaisland.glogc :as log]))

(defn wrap-rate-limit
  "Decorator for slack request functions which handles rate limiting.
  The resulting function will automatically block the thread and retry when
  necessary."
  [f]
  (fn invoke [& args]
    (let [response (apply f args)]
      (if (= 429 (:status response))
        (let [wait-for (Integer/parseInt (get-in response [:headers "retry-after"]))]
          (log/info :slack-api/rate-limited {:retry-after-seconds wait-for})
          (Thread/sleep (* (inc wait-for) 1000))
          (apply invoke args))
        response))))

(defn wrap-paginate
  "Decorator for slack request functions which handles pagination

  Returns a lazy seq of the collection, or the response map if the first request
  fails. `k` is the key slack uses in the response to denote the result
  collection.

  When a non first request fails, the lazy seq of the collection before that
  request will be returned. If the error-logger in injected, the failed request's
  response will be record by the error-logger.
  Note: error-logger can be nil if it is not needed.

  If the `opts` contains a limit parameter, just use it. If the `opts` does not
  contain a limit parameter, assign the limit parameter as value 1000. The limit
  parameter maximum is 1000 according to the document
  https://api.slack.com/docs/pagination

  Needs to come after [[wrap-result]], because this assumes it gets just the
  parsed JSON body back."
  [error-logger k f]
  (fn paginate
    ([conn]
     (paginate conn {}))
    ([conn opts]
     (let [opts* (if (:limit opts)
                   opts
                   (assoc opts :limit 1000))
           resp (f conn opts*)
           lazy-f (fn lazy-f [{:strs [ok] :as resp}]
                    (if ok
                      (let [cursor (get-in resp ["response_metadata" "next_cursor"])]
                        (lazy-cat (get resp (name k))
                                  (when-not (empty? cursor)
                                    (lazy-f (f conn (merge opts* {:cursor cursor}))))))
                      (when error-logger
                        (error-logger resp))))]
       (if (get resp "ok")
         (lazy-f resp)
         resp)))))

(defn wrap-retry-exception
  "Occasionally we get a low-level SocketException or IOException, sleep and retry at most `retries` times"
  [retries f]
  (fn self
    ([conn]
     (self conn {}))
    ([conn opts]
     (try
       (f conn opts)
       (catch java.io.IOException e
         (log/error :slack-api/io-exception {:opts opts} :exception e)
         (let [retries (:retries opts 0)]
           (if (< retries 5)
             (do
               (Thread/sleep (* retries retries 1000))
               (self conn (update opts :retries (fnil inc 0))))
             (do
               (log/error :slack-api/retries-exhausted {:retries retries} :exception e)
               (throw e)))))))))

(defn wrap-result
  "Grab the \"result\" key from the response (= parsed json)"
  [f]
  (fn self
    ([conn]
     (self conn nil))
    ([conn opts]
     (:result (f conn opts)))))

(defn wrap-get [f k]
  (fn self
    ([conn]
     (self conn nil))
    ([conn opts]
     (let [resp (f conn opts)]
       (if (get resp "ok")
         (get resp k)
         resp)))))

(defn wrap-coerce
  "Apply a coercion function to each element in the resulting collection."
  [f norm-fn]
  (fn self
    ([conn]
     (self conn nil))
    ([conn opts]
     (let [resp (f conn opts)]
       (if (sequential? resp)
         (map norm-fn resp)
         resp)))))
