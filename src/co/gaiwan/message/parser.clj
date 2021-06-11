(ns co.gaiwan.message.parser
  (:require [clj-antlr.core :as antlr]
            [clojure.java.io :as io]))

(def parse (antlr/parser (slurp (io/resource "markdown.g4"))))

(time (parse "_*bbb*_"))
