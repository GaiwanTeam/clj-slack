(ns co.gaiwan.slack.api.middleware
  "Decorators for API request functions that handle cross cutting concerns"
  (:require [lambdaisland.glogc :as log]))

(defn wrap-rate-limit
  "Decorator for slack request functions which handles rate limiting.
  The resulting function will automatically block the thread and retry when
  necessary."
  [f]
  (fn invoke [& args]
    (try
      (apply f args)
      (catch clojure.lang.ExceptionInfo ex
        (let [data (ex-data ex)]
          (if (= 429 (:status data))
            (let [wait-for (Integer/parseInt (get-in data [:headers "retry-after"]))]
              (log/info :slack-api/rate-limited {:retry-after-seconds wait-for})
              (Thread/sleep (* (inc wait-for) 1000))
              (apply invoke args))
            (throw ex)))))))

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
  https://api.slack.com/docs/pagination"
  [error-logger k f]
  (fn paginate
    ([conn]
     (paginate conn {}))
    ([conn opts]
     (let [opts* (if (:limit opts)
                   opts
                   (assoc opts :limit 1000))
           resp (f conn opts*)
           lazy-f (fn lazy-f [{:keys [ok] :as resp}]
                    (if ok
                      (let [cursor (get-in resp [:response_metadata :next_cursor])]
                        (lazy-cat (get resp k)
                                  (when-not (empty? cursor)
                                    (lazy-f (f conn (merge opts* {:cursor cursor}))))))
                      (when error-logger
                        (error-logger resp))))]
       (if (:ok resp)
         (lazy-f resp)
         resp)))))

(defn wrap-retry-exception
  "Occasionally we get a low-level SocketException or IOException, sleep and retry at most `retries` times"
  [retries f]
  (fn do-req
    ([conn]
     (do-req conn {}))
    ([conn opts]
     (let [count (volatile! 0)]
       (try
         (f conn opts)
         (catch java.io.IOException e
           (log/error :slack-api/io-exception {:opts opts :retries @count} :exception e)
           (if (< @count retries)
             (do
               (vswap! count inc)
               (Thread/sleep (* @count @count 1000))
               (do-req conn opts))
             (do
               (log/error :slack-api/retries-exhausted {:retries @count} :exception e)
               (throw e)))))))))
