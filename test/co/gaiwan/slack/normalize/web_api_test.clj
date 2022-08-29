(ns co.gaiwan.slack.normalize.web-api-test
  #_(:require [co.gaiwan.slack.normalize.web-api :as web-api]
              [clojure.test :refer :all]
              [clojure.java.io :as io]
              [co.gaiwan.slack.schema :as schema]
              [co.gaiwan.data-tools.schema-derivations :as schema-derivations]
              [malli.core :as malli]
              [malli.error :as malli-error]))

#_(defn validate! [type value]
    (when-not (malli/validate type value {:registry schema/malli-registry})
      (throw (ex-info (str "Invalid " type)
                      (-> (malli/explain type value {:registry schema/malli-registry})
                          (malli-error/humanize))))))

#_(defn demo-users []
    (read-string (slurp (io/resource "co/gaiwan/slack/demo_users.edn"))))
#_
(deftest users-are-valid
  (is (= nil
         (doseq [user (demo-users)]
           (validate! :slack/user (web-api/user user))))))
