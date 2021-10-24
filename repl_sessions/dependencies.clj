(ns dependencies
  (:require [lambdaisland.classpath :as licp]))

(licp/update-classpath! '{:extra {:deps {lambdaisland/edn-lines {:local/root "/home/arne/github/lambdaisland/edn-lines"}
                                         com.cognitect/transit-clj {:mvn/version "1.0.324"}
                                         }}})
