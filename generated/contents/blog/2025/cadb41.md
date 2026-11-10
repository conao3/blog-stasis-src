Title: Clojure標準ライブラリ - clojure.data
Date: 2025-02-26
Draft: true



# 依存

Clojure標準ライブラリのため、追加の依存は必要ありません。


# require

この記事では以下で `require` されているものとします

```clojure
(require '[clojure.data :as data])
```


# 概観

```clojure
(-> (->> (ns-publics 'clojure.data)
         (map (fn [[key val]]
                [key (cond
                       (fn? (var-get val)) :fn
                       :else :else)]))
         (group-by second))
    (update-vals (partial map first)))
;;=> {:fn (diff
;;         equality-partition
;;         diff-similar),
;;    :else (EqualityPartition
;;           Diff)}
```

<https://clojure.github.io/clojure/clojure.data-api.html>

`diff` のみが公開APIで、その他の関数は実装詳細だという旨が記載されています。


# 使い方


## diff

```clojure
(defn diff
  "Recursively compares a and b, returning a tuple of
  [things-only-in-a things-only-in-b things-in-both].
  Comparison rules:

  * For equal a and b, return [nil nil a].
  * Maps are subdiffed where keys match and values differ.
  * Sets are never subdiffed.
  * All sequential things are treated as associative collections
    by their indexes, with results returned as vectors.
  * Everything else (including strings!) is treated as
    an atom and compared for equality."
  {:added "1.3"}
  [a b]
  (if (= a b)
    [nil nil a]
    (if (= (equality-partition a) (equality-partition b))
      (diff-similar a b)
      (atom-diff a b))))
```

```clojure
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
                        {:a 1 :b {:c 2 :d 4}})))))
```

-   `equal` の場合、 `[nil nil a]` が返却される。
    これは実装から明らかですね
-   マップの場合、キーが一致し値が異なる部分については再帰的に差分を取る。
    
    ```clojure
    (t/is (= [{:b {:d 3}} {:b {:d 4}} {:a 1, :b {:c 2}}]
             (data/diff {:a 1 :b {:c 2 :d 3}}
                        {:a 1 :b {:c 2 :d 4}})))
    ```
    
    これは直感的な動作になっていますね
-   セットは再帰的に差分を取らない。
    
    ```clojure
    (t/is (= [#{[1 2]} #{[2 3] 5} #{4 3}]
             (data/diff #{[1 2] 3 4}
                        #{[2 3] 3 4 5})))
    ```
    
    セットの場合、要素がコレクションだったとしても再帰的に比較しません。そのためこの例では `1` がセット内のリスト内の共通要素ですが、全体が差分として計算されています。
    セットの場合、順序が存在しないのでそもそも `[1 2]` と `[2 3]` を比較しなければならないということを判断するのが難しいです。

