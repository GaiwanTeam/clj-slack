(ns co.gaiwan.slack.normalize.web-api-test
  (:require [co.gaiwan.slack.normalize.web-api :as web-api]
            [clojure.test :refer :all]
            [clojure.java.io :as io]
            [co.gaiwan.slack.schema :as schema]))

(defn demo-users []
  (read-string (slurp (io/resource "co/gaiwan/slack/demo_users.edn"))))

(deftest users-are-valid
  (is (= nil
         (doseq [user (demo-users)]
           (schema/validate! :slack/user (web-api/normal-user user))))))
