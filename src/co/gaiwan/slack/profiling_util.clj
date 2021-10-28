(ns co.gaiwan.slack.profiling-util
  "Helper macros for quick micro benchmarking.")

(defmacro time-with-label
  "Evaluates expr, prints the execution time with the supplied `label`, returns the value of expr."
  [label expr]
  `(let [start# (. System (nanoTime))
         ret# ~expr]
     (prn (str ~label ": " (/ (double (- (. System (nanoTime)) start#)) 1000000.0) " msecs"))
     ret#))

(defmacro timecount
  "Given a form which returns a sequence, evaluate the form, count the number of
  elements in the result, and print out how much time was spent in total, as
  well as how many elements per second were processed/returned."
  [form]
  `(let [start# (System/nanoTime)
         cnt# (count ~form)
         end# (System/nanoTime)
         ms# (/ (- end# start#) 1e6)]
     (println (format "%d in %fms = %d/sec" (long cnt#) ms# (long (/ cnt# ms# 0.001))))
     cnt#))
