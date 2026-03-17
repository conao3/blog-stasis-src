(ns conao3.sample-test
  (:require
    [clojure.test :as t]
    [conao3.sample :as c]))


(t/deftest add-test
  (t/is (= 3 (c/add 1 2))))
