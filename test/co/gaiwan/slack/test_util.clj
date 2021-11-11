(ns co.gaiwan.slack.test-util
  (:import (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(defn temp-dir
  "Create a temporary directory, return the path"
  ([]
   (temp-dir "gaiwan-clj-slack"))
  ([prefix]
   (str (Files/createTempDirectory prefix (make-array FileAttribute 0)))))
