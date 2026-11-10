(ns sample.clojure.data-test
  (:require
   [clojure.data :as data]
   [clojure.test :as t])
  (:import
   java.util.HashSet))

(t/deftest diff-test
  (t/testing "official"
    (t/is (= [nil nil nil] (data/diff nil nil)))
    (t/is (= [1 2 nil] (data/diff 1 2)))
    (t/is (= [nil nil [1 2 3]] (data/diff [1 2 3] '(1 2 3))))
    (t/is (= [1 [:a :b] nil] (data/diff 1 [:a :b])))
    (t/is (= [{:a 1} :b nil] (data/diff {:a 1} :b)))
    (t/is (= [:team #{:p1 :p2} nil] (data/diff :team #{:p1 :p2})))
    (t/is (= [{0 :a} [:a] nil] (data/diff {0 :a} [:a])))
    (t/is (= [nil [nil 2] [1]] (data/diff [1] [1 2])))
    (t/is (= [nil nil [1 2]] (data/diff [1 2] (into-array [1 2]))))
    (t/is (= [#{:a} #{:b} #{:c :d}] (data/diff #{:a :c :d} #{:b :c :d})))
    (t/is (= [nil nil {:a 1}] (data/diff {:a 1} {:a 1})))
    (t/is (= [{:a #{2}} {:a #{4}} {:a #{3}}] (data/diff {:a #{2 3}} {:a #{3 4}})))
    (t/is (= [#{1} #{3} #{2}] (data/diff (HashSet. [1 2]) (HashSet. [2 3]))))
    (t/is (= [nil nil [1 2]] (data/diff [1 2] (into-array [1 2]))))
    (t/is (= [nil nil [1 2]] (data/diff (into-array [1 2]) [1 2])))
    (t/is (= [{:a {:c [1]}} {:a {:c [0]}} {:a {:c [nil 2] :b 1}}] (data/diff {:a {:b 1 :c [1 2]}} {:a {:b 1 :c [0 2]}})))
    (t/is (= [{:a nil} {:a false} {:b nil :c false}] (data/diff {:a nil :b nil :c false} {:a false :b nil :c false}))))

  (t/testing "map"
    (t/is (= [{:b {:d 3}} {:b {:d 4}} {:a 1, :b {:c 2}}]
             (data/diff {:a 1 :b {:c 2 :d 3}}
                        {:a 1 :b {:c 2 :d 4}}))))

  (t/testing "set"
    (t/is (= [#{[1 2]} #{[2 3] 5} #{4 3}]
             (data/diff #{[1 2] 3 4}
                        #{[2 3] 3 4 5})))))
