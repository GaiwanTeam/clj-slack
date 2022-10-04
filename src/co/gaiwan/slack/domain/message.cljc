(ns co.gaiwan.slack.domain.message
  "Schema and logic for manipulating Message data")

(def ?Timestamp [:re #"\d{10}\.\d{6}"])

(def ?Message
  [:map
   [:message/timestamp ?Timestamp]
   [:message/channel-id string?]
   [:message/user-id string?]
   [:message/text string?]
   [:message/thread-ts {:optional true} ?Timestamp]
   [:message/thread-broadcast? {:optional true} boolean?]
   [:message/reply-timestamps [:set ?Timestamp]]
   [:message/deleted? {:optional true} boolean?]
   [:message/system? {:optional true} boolean?]
   [:message/image? {:optional true} boolean?]
   [:message/reactions {:optional true} [:map-of string? int?]]])

(defn ts->micros
  "If `s` is a ts string, converts it to Long. Else, returns nil."
  [s]
  (when-let [[_ seconds micros] (re-find #"(\d{10})\.(\d{6})" s)]
    (parse-long (str seconds micros))))

(defn reaction-count [{:message/keys [reactions]}]
  (reduce + (vals reactions)))

(defn replies-count [{:message/keys [reply-timestamps]}]
  (count reply-timestamps))

(defn relevancy [message {:keys [start-time-ms exponent
                                 reaction-factor replies-factor]
                          :or {exponent 1.2
                               reaction-factor 2
                               replies-factor 1}}]
  (let [reaction-count (reaction-count message)
        replies-count  (replies-count message)
        seconds-elapsed (/ (- (* 1000 start-time-ms)
                              (ts->micros (:message/timestamp message)))
                           1000)
        score          (+ (* reaction-count reaction-factor)
                          (* replies-count replies-factor))]
    (* 100000
       (/ score (Math/pow (inc seconds-elapsed) exponent)))))
