(ns co.gaiwan.slack.socket.api
  "Lightweight implementation for connecting to the Slack Socket API

  https://api.slack.com/apis/connections/socket-implement"
  (:require [hato.client :as http]
            [clojure.data.json :as json]
            [lambdaisland.glogc :as log])
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]
           java.net.URI
           org.java_websocket.client.WebSocketClient))

(defn get-wss-url
  "Given an xapp-... token, request a websocket URL"
  [token]
  (let [response (http/post "https://slack.com/api/apps.connections.open"
                            {:headers {"Content-type" "application/x-www-form-urlencoded"
                                       "Authorization" (str "Bearer " token)}})]
    (if (not= 200 (:status response))
      (throw (ex-info "Open slack socket connection failed"
                      {:response response}))
      (let [{:strs [ok url]} (json/read-str (:body response))]
        (if ok
          url
          (throw (ex-info "Open slack socket connection failed"
                          {:response response})))))))

(defn- noop [& _])

(defn ws-send
  "Send a websocket back, takes EDN, will serialize to JSON"
  [^WebSocketClient ws-client msg]
  (.send ws-client (json/write-str msg)))

(defn ws-connect
  "Connect to a websocket, takes a `:uri` and handlers, mainly `:on-message`"
  ^WebSocketClient
  [{:keys [uri on-open on-message on-error on-close whoami]
    :or {on-open noop
         on-message noop
         on-error noop
         on-close noop
         whoami noop}
    :as opts}]
  (let [conn (proxy [WebSocketClient clojure.lang.IMeta] [(URI. (str uri))]
               (onOpen [handshake]
                 (log/debug :websocket/open handshake)
                 (on-open this handshake))
               (onError [ex]
                 (log/warn :websocket/error true :exception ex)
                 (on-error this ex))
               (onMessage [message]
                 (log/trace :websocket/message message)
                 (let [{:strs [envelope_id payload] :as parsed}
                       (json/read-str message)]
                   (on-message this parsed)
                   (when envelope_id
                     ;; slack requires acknowledgement like this
                     (ws-send this {"envelope_id" envelope_id}))))
               (onClose [code reason remote?]
                 (log/info :websocket/close {:code code :reason reason :remote? remote?})
                 (on-close this {:code code :reason reason :remote? remote?}))
               (meta []
                 opts))]
    (when-not (.connectBlocking conn 2 java.util.concurrent.TimeUnit/SECONDS)
      (throw (ex-info "Failed connecting" {:uri uri})))
    conn))


(comment
  (def messages (atom []))
  (def token "xapp-1-...")
  (defonce url (get-wss-url token))
  (def conn
    (ws-connect {:uri url
                 :on-open (fn [_ hs]
                            (prn [:on-open hs]))
                 :on-message (fn [_ msg]
                               (prn [:msg msg])
                               (swap! messages conj msg))}))

  (spit "resources/co/gaiwan/slack/message_event.edn"
        (with-out-str
          (clojure.pprint/pprint
           (last @messages)))))
