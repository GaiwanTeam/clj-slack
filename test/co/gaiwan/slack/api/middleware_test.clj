(ns co.gaiwan.slack.api.middleware-test
  (:require [clojure.test :refer :all]
            [io.pedestal.log :as log]
            [co.gaiwan.slack.api.middleware :as mw]))

(def logger #(log/info :slack-api/pagination-error %))

(defn req-failed
  [_ opts]
  {:ok false
   :error "wrong scope"})

(defn req-with-one-cursor
  [_ opts]
  (cond
    (nil? (:cursor opts)) {:ok true
                           :items [:1 :2 :3]
                           :response_metadata {:next_cursor "c-1"}}
    (= "c-1" (:cursor opts)) {:ok true
                              :items [:4 :5 :6]
                              :response_metadata {:next_cursor ""}}))

(defn req-with-cursor-and-failed
  [_ opts]
  (cond
    (nil? (:cursor opts)) {:ok true
                           :items [:11 :22 :33]
                           :response_metadata {:next_cursor "c-1"}}
    (= "c-1" (:cursor opts)) {:ok true
                              :items [:44 :55 :66]
                              :response_metadata {:next_cursor "c-2"}}
    (= "c-2" (:cursor opts)) {:ok false
                              :error "wrong scope"}))
(def final-cursor (atom nil))

(defn req-ext
  [_ opts]
  (log/info :fake-req-arg opts)
  (let [new-cursor (fn [s] (let [n (read-string s)]
                             (str (+ 10 n))))
        new-range (fn [s]
                    (let [a (read-string s)
                          b (+ 10 a)]
                      (vec (range a b))))]
    (cond
      (nil? (:cursor opts)) (do
                              (reset! final-cursor "10")
                              {:ok true
                               :items (vec (range 0 10))
                               :response_metadata {:next_cursor "10"}})
      (= "100" (:cursor opts)) (do
                                 (reset! final-cursor nil)
                                 {:ok false
                                  :error "wrong scope"})
      (string? (:cursor opts)) (do
                                 (reset! final-cursor (new-cursor (:cursor opts)))
                                 {:ok true
                                  :items (new-range (:cursor opts))
                                  :response_metadata {:next_cursor (new-cursor (:cursor opts))}}))))

(let  [conn {}
       opts {}
       f1 (mw/wrap-paginate logger :items req-failed)
       f2 (mw/wrap-paginate logger :items req-with-one-cursor)
       f3 (mw/wrap-paginate logger :items req-with-cursor-and-failed)
       fx (mw/wrap-paginate logger :items req-ext)]
  (deftest test-pagination-with-fake-request
    (testing "test with request failed immediately"
      (is (= (f1 conn opts)
             {:error "wrong scope", :ok false})))
    (testing "test with request with one cursor"
      (is (= (f2 conn opts)
             (list :1 :2 :3 :4 :5 :6))))
    (testing "test with request with cursors and then failed"
      (is (= (f3 conn opts)
             (list :11 :22 :33 :44 :55 :66))))
    (testing "test with extensible request lazy-evaluated"
      (is (= (take 53 (fx conn opts))
             (range 0 53)))
      (is (= @final-cursor
             "60")))
    (testing "test with extensible request eager-evaluated"
      (is (= (fx conn opts)
             (range 0 100))))))

