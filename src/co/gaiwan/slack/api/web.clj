(ns co.gaiwan.slack.api.web
  (:require [clojure.data.json :as json]
            [lambdaisland.glogc :as log]
            [hato.client :as http]
            [lambdaisland.uri :as uri]))

(defn- verify-api-url!
  [connection]
  (assert
   (and (string? (:api-url connection))
        (and (not (empty? (:api-url connection)))
             (not (nil? (re-find #"^https?:\/\/" (:api-url connection))))))
   (str "clj-slack: API URL is not valid. :api-url has to be a valid URL (https://slack.com/api usually), but is " (pr-str (:api-url connection)))))

(defn- verify-token!
  [connection]
  (assert
   (and (string? (:token connection))
        (not (empty? (:token connection))))
   (str "clj-slack: Access token is not valid. :token has to be a non-empty string, but is " (pr-str (:token connection)))))

(defn- verify-conn!
  "Checks the connection map"
  [conn]
  (verify-api-url! conn)
  (when (not (contains? conn :skip-token-validation))
    (verify-token! conn))
  nil)

(defn error?
  "Is the response an error?
  This checks for a couple of different cases that might arise. Generally it's
  advisable to always check if a response is an error before trying to use its
  results.
  Slack returns an :ok true/false key in every response. For paginated
  collection responses we normally unwrap the outer map, unless (= :ok false),
  so this should also work on collection responses.
  For rare exceptions where Slack returns a non-200 response with an empty body
  we simply return `:error`."
  [response]
  (or (= :error response)
      (and (map? response) (false? (:ok response)))))

(defn- send-get-request
  "Sends a GET http request with formatted params.
  Optional request options can be specified which will be passed to `hato`
  without any changes."
  [url {:keys [token path]} & [opts]]
  (log/debug :slack-api/GET (str url path))
  (let [full-url (str url path)
        response (http/get full-url (merge {:oauth-token token
                                            :throw-exceptions? false}) opts)
        result (if-let [body (:body response)]
                 (assoc response :result (json/read-str body :key-fn clojure.core/keyword))
                 ;; Slack normally returns a JSON body with `:ok false`, so this is for
                 ;; truly exceptional cases
                 (assoc response :result :error))]
    (when (error? (:result result))
      (log/error :slack-api/error-response response))
    result))

(defn- request-options
  "Extracts request options from slack connection map.
  Provides sensible defaults for timeouts."
  [connection]
  (let [default-options {:conn-timeout 60000
                         :socket-timeout 60000}]
    (merge default-options
           (dissoc connection :api-url :token))))

(defn- build-params
  "Builds the full URL (endpoint + params)"
  ([conn endpoint query-map]
   (verify-conn! conn)
   {:token (:token conn)
    :path  (-> (uri/uri (str "/" endpoint))
               (uri/assoc-query* query-map)
               str)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; The request API

(defn slack-request
  ([conn endpoint]
   (slack-request conn endpoint {}))
  ([conn endpoint opt]
   (verify-conn! conn)
   (let [url (:api-url conn)
         params (build-params conn endpoint opt)]
     (send-get-request url params (request-options conn)))))
