(ns co.gaiwan.slack.socket.api
  "Lightweight implementation for connecting to the Slack Socket API

  https://api.slack.com/apis/connections/socket-implement

  Main usage pattern, call `ws-connect` with a token, then register listeners.
  "
  (:require [hato.client :as http]
            [charred.api :as json]
            [lambdaisland.glogc :as log]
            [co.gaiwan.slack.protocols :as protocols])
  (:import (java.io ByteArrayInputStream ByteArrayOutputStream)
           (java.net URI)
           (org.java_websocket.client WebSocketClient)))

(defn get-wss-url
  "Given an xapp-... token, request a websocket URL"
  [token]
  (let [response (http/post "https://slack.com/api/apps.connections.open"
                            {:headers {"Content-type" "application/x-www-form-urlencoded"
                                       "Authorization" (str "Bearer " token)}})]
    (if (not= 200 (:status response))
      (throw (ex-info "Open slack socket connection failed"
                      {:response response}))
      (let [{:strs [ok url]} (json/read-json (:body response))]
        (if ok
          url
          (throw (ex-info "Open slack socket connection failed"
                          {:response response})))))))

(defn- noop [& _])

(defn ws-send
  "Send a websocket back, takes EDN, will serialize to JSON"
  [^WebSocketClient ws-client msg]
  (.send ws-client (json/write-json-str msg)))

(defn ws-connect*
  "Connect to a websocket, takes a `:uri` and handlers, mainly `:on-message`.
  Low-level, does not handle reconnects."
  ^WebSocketClient
  [{:keys [uri on-open on-message on-error on-close keywordize?]
    :or {on-open noop
         on-message noop
         on-error noop
         on-close noop}
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
                 (let [{:keys [envelope_id payload] :as parsed}
                       (json/read-json message :key-fn (if keywordize? keyword identity))]
                   (on-message this parsed)
                   (when envelope_id
                     ;; slack requires acknowledgement like this
                     (ws-send this {"envelope_id" envelope_id}))))
               (onClose [code reason remote?]
                 (log/info :websocket/close {:code code :reason reason :remote? remote?})
                 (on-close this {:code code :reason reason :remote? remote?}))
               (meta []
                 opts))]
    ;; Disable ping/pong check, Slack does not seem to adhere to this
    (.setConnectionLostTimeout conn 0)
    (when-not (.connectBlocking conn 2 java.util.concurrent.TimeUnit/SECONDS)
      (throw (ex-info "Failed connecting" {:uri uri})))
    conn))

(defn unwrap-message
  "Unwrap 'regular' event-api messages from slack, since they have two wrapping
  maps with metadata. We preserve the full context on the metadata for
  debugging. Leave any other type of event/message as-is."
  [msg]
  (if-let [evt (and (= "events_api" (get msg "type"))
                    (get-in msg ["payload" "event"]))]
    (with-meta evt
      (update msg "payload" dissoc "event"))
    msg))

(defn ws-connect
  "'smart' websocket connect. Handles reconnects, and exposes all kinds of
  events via a unified listener interface. Listeners can be passed in at
  construction time (map from key to listener-fn), or added/removed later via
  [[add-listener]]/[[remove-listener]]."
  [{:keys [token
           debug-reconnects?
           listeners
           keywordize?]
    :or {listeners {}}
    :as opts}]
  {:pre [token]}
  (let [!conn (atom nil)
        listeners (atom listeners)
        reconnect!
        (fn reconnect! []
          (swap!
           !conn
           (fn [conn]
             (if (= ::closed conn)
               ::closed
               (do
                 (when conn
                   (.close conn))
                 (ws-connect* {:uri (cond-> (get-wss-url token)
                                      debug-reconnects?
                                      (str "&debug_reconnects=true"))
                               :keywordize? keywordize?
                               :on-open (fn [conn handshake]
                                          (run! #(% {:type "websocket"
                                                     :subtype "open"
                                                     :handshake handshake})
                                                (vals @listeners)))
                               :on-message (fn [conn msg]
                                             (let [msg (unwrap-message msg)]
                                               (run! #(% msg)
                                                     (vals @listeners)))
                                             (when (= "disconnect" (:type msg))
                                               (reconnect!)))
                               :on-error (fn [conn err]
                                           (run! #(% {:type "websocket"
                                                      :subtype "error"
                                                      :error err})
                                                 (vals @listeners)))
                               :on-close (fn [conn flags]
                                           (run! #(% {:type "websocket"
                                                      :subtype "close"
                                                      :flags flags})
                                                 (vals @listeners))
                                           (reconnect!))}))))))]
    (reconnect!)
    (with-meta
      (reify java.io.Closeable
        (close [this]
          (let [conn @!conn]
            (when (and conn (not= ::closed conn))
              (.close conn)
              (reset! !conn ::closed))))
        protocols/EventSource
        (add-listener [this watch-key listener]
          (swap! listeners assoc watch-key listener))
        (remove-listener [this watch-key]
          (swap! listeners dissoc watch-key)))
      {:!conn !conn
       :listeners listeners
       :token token})))

(comment
  (def messages (atom []))
  (def token "xapp-...")
  ;; (def url (str (get-wss-url token)
  ;;               "&debug_reconnects=true"))
  (def conn
    (ws-connect {:token token
                 :debug-reconnects? true
                 :listeners {:x (fn [msg]
                                  (prn [:msg msg])
                                  (swap! messages conj msg))}}))

  ;; changes weren't being picked up or something... restarted the repl

  (meta conn)

  (map (juxt :type :subtype) @messages)
  (map (comp :event :payload) @messages)
  (tap> @messages)
  (.close conn)
  (spit "resources/co/gaiwan/slack/message_event.edn"
        (with-out-str
          (clojure.pprint/pprint
           (last @messages)))))
