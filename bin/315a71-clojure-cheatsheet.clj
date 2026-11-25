(ns cheatsheet
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util Base64]))

(defn probe
  "An unchunked seq of 0, 1, 2, ... (n elements when n is given) that records
  which indices were realized."
  ([] (probe nil))
  ([n]
   (let [seen (atom (sorted-set))
         step (fn step [i]
                (lazy-seq
                 (when (or (nil? n) (< i n))
                   (swap! seen conj i)
                   (cons i (step (inc i))))))]
     [(step 0) seen])))

(defn realized-by
  "Calls f on a probe seq and returns the set of indices realized by f."
  ([f] (realized-by nil f))
  ([n f]
   (let [[s seen] (probe n)]
     (f s)
     @seen)))

(defn lazy-on-infinite?
  "True when taking 3 elements from (f infinite-seq) returns."
  [f]
  (= 3 (count (take 3 (f (first (probe)))))))

(defn sp [& xs] (str/join "\\ " (remove nil? xs)))
(defn sq [& xs] (str "(" (apply sp xs) ")"))
(defn vc [& xs] (str "[" (apply sp xs) "]"))
(defn st [& xs] (str "\\#\\{" (apply sp xs) "\\}"))
(defn mp [& xs] (str "\\{" (str/join ",\\ " xs) "\\}"))
(defn ul [& xs] (str "\\underline{" (apply sp xs) "}"))
(defn ub [body label] (str "\\underbrace{" body "}_{" label "}"))
(defn t [s] (str "\\text{" s "}"))
(defn kw [s] (str "\\mathtt{" s "}"))
(def dots "\\ldots")
(def nil' (kw "nil"))
(def true' (kw "true"))
(def false' (kw "false"))
(defn wh [& xs] (str "\\quad " (t "wh. ") (apply str xs)))
(defn lam [args body] (str args " \\mapsto " body))
(defn cases [& rows]
  (str "\\begin{array}{ll}"
       (str/join " \\\\ " (map (fn [[v c]] (str v " & " c)) (partition 2 rows)))
       "\\end{array}"))
(defn first-wh [& cond'] (str "i" (t " is the first index with ") (apply str cond')))

(def A (sq "a_0" "a_1" dots))
(def An (sq "a_0" dots "a_{n-1}"))
(def Bm (sq "b_0" dots "b_{m-1}"))
(def P (str "p : x \\to " (t "bool")))
(def F "f : x \\to y")
(def F2 "f : \\mathit{acc}\\ x \\to \\mathit{acc}")
(defn ap [& xs] (apply sq xs))
(def pa-false (str (ap "p" "a_i") " = " false'))
(def pa-true (str (ap "p" "a_i") " = " true'))

(def sections
  [{:id "making"
    :rows
    [{:name "seq"
      :cases [{:args [(vc "a_0" "a_1" dots)] :out A}
              {:args [(vc)] :out nil'}]
      :checks [(= (seq [1 2]) '(1 2)) (nil? (seq [])) (nil? (seq nil))]}
     {:name "range" :lazy true
      :cases [{:args [] :out (sq "0" "1" "2" dots)}
              {:args ["n"] :out (sq "0" "1" dots "n-1")}
              {:args ["i" "j"] :out (sq "i" "i+1" dots "j-1")}
              {:args ["i" "j" "s"] :out (str (sq "i" "i+s" "i+2s" dots) (wh (t "every element is ") "< j"))}]
      :checks [(= (take 3 (range)) [0 1 2]) (= (range 3) [0 1 2]) (= (range 2 5) [2 3 4]) (= (range 0 7 3) [0 3 6])]}
     {:name "repeat" :lazy true
      :cases [{:args ["x"] :out (sq "x" "x" dots)}
              {:args ["n" "x"] :out (sq (ub (sp "x" dots "x") "n"))}]
      :checks [(= (take 2 (repeat :x)) [:x :x]) (= (repeat 3 :x) [:x :x :x])]}
     {:name "repeatedly" :lazy true
      :cases [{:args ["f : () \\to y"] :out (sq "(f)" "(f)" dots)}
              {:args ["n" "f : () \\to y"] :out (sq (ub (sp "(f)" dots "(f)") "n"))}]
      :checks [(= (repeatedly 2 (constantly 1)) [1 1]) (= 3 (count (take 3 (repeatedly (constantly 1)))))]}
     {:name "iterate" :lazy true
      :cases [{:args ["f : x \\to x" "x"] :out (sq "x" (ap "f" "x") (ap "f" (ap "f" "x")) dots)}]
      :checks [(= (take 4 (iterate inc 0)) [0 1 2 3])]}
     {:name "keys"
      :cases [{:args [(mp (sp "k_0" "v_0") (sp "k_1" "v_1") dots)] :out (sq "k_0" "k_1" dots)}
              {:args [(mp)] :out nil'}]
      :checks [(= (keys (array-map :a 1 :b 2)) [:a :b]) (nil? (keys {}))]}
     {:name "vals"
      :cases [{:args [(mp (sp "k_0" "v_0") (sp "k_1" "v_1") dots)] :out (sq "v_0" "v_1" dots)}
              {:args [(mp)] :out nil'}]
      :checks [(= (vals (array-map :a 1 :b 2)) [1 2]) (nil? (vals {}))]}
     {:name "rseq"
      :cases [{:args [(vc "a_0" dots "a_{n-1}")] :out (sq "a_{n-1}" dots "a_0")}
              {:args [(vc)] :out nil'}]
      :checks [(= (rseq [1 2 3]) [3 2 1]) (nil? (rseq []))]}]}

   {:id "position"
    :rows
    [{:name "take" :lazy true :xf true
      :cases [{:args ["n" (sq "a_0" dots "a_{n-1}" (ul "a_n" dots))] :out (sq "a_0" dots "a_{n-1}")}]
      :checks [(= (take 2 [1 2 3]) [1 2]) (= (take 5 [1 2]) [1 2])
               (= #{0 1} (realized-by #(doall (take 2 %))))]}
     {:name "drop" :lazy true :xf true
      :cases [{:args ["n" (sq "a_0" dots "a_{n-1}" "a_n" dots)] :out (sq "a_n" dots)}]
      :checks [(= (drop 2 [1 2 3]) [3]) (= (drop 5 [1 2]) [])]}
     {:name "take-last"
      :cases [{:args ["k" An] :out (sq "a_{n-k}" dots "a_{n-1}")}
              {:args ["k" (sq)] :out nil'}]
      :checks [(= (take-last 2 [1 2 3]) [2 3]) (nil? (take-last 2 []))]}
     {:name "drop-last" :lazy true
      :cases [{:args [An] :out (sq "a_0" dots "a_{n-2}")}
              {:args ["k" An] :out (sq "a_0" dots "a_{n-k-1}")}]
      :checks [(= (drop-last [1 2 3]) [1 2]) (= (drop-last 2 [1 2 3]) [1])]}
     {:name "butlast"
      :cases [{:args [An] :out (sq "a_0" dots "a_{n-2}")}
              {:args [(sq "a_0")] :out nil'}]
      :checks [(= (butlast [1 2 3]) [1 2]) (nil? (butlast [1]))]}
     {:name "rest"
      :cases [{:args [(sq "a_0" (ul "a_1" dots))] :out (sq "a_1" dots)}
              {:args [(sq "a_0")] :out (sq)}
              {:args [nil'] :out (sq)}]
      :checks [(= (rest [1 2]) [2]) (= (rest [1]) ()) (= (rest nil) ())
               (= #{0} (realized-by #(do (rest %) nil)))]}
     {:name "next"
      :cases [{:args [(sq "a_0" "a_1" (ul dots))] :out (sq "a_1" dots)}
              {:args [(sq "a_0")] :out nil'}
              {:args [nil'] :out nil'}]
      :checks [(= (next [1 2]) [2]) (nil? (next [1])) (nil? (next nil))
               (= #{0 1} (realized-by #(do (next %) nil)))]}
     {:name "nnext"
      :cases [{:args [(sq "a_0" "a_1" "a_2" dots)] :out (sq "a_2" dots)}
              {:args [(sq "a_0" "a_1")] :out nil'}]
      :checks [(= (nnext [1 2 3]) [3]) (nil? (nnext [1 2]))]}
     {:name "nthrest"
      :cases [{:args [(sq "a_0" dots "a_{k-1}" "a_k" dots) "k"] :out (sq "a_k" dots)}
              {:args [An "k \\ge n"] :out (sq)}]
      :checks [(= (nthrest [1 2 3] 2) [3]) (= (nthrest [1 2] 5) ())]}
     {:name "nthnext"
      :cases [{:args [(sq "a_0" dots "a_{k-1}" "a_k" dots) "k"] :out (sq "a_k" dots)}
              {:args [An "k \\ge n"] :out nil'}]
      :checks [(= (nthnext [1 2 3] 2) [3]) (nil? (nthnext [1 2] 5))]}
     {:name "take-nth" :lazy true :xf true
      :cases [{:args ["k" A] :out (sq "a_0" "a_k" "a_{2k}" dots)}]
      :checks [(= (take-nth 2 (range 7)) [0 2 4 6])]}]}

   {:id "predicate"
    :rows
    [{:name "filter" :lazy true :xf true
      :cases [{:args [P A] :out (str (sq "a_i" dots) (wh pa-true))}]
      :checks [(= (filter even? (range 6)) [0 2 4])]}
     {:name "remove" :lazy true :xf true
      :cases [{:args [P A] :out (str (sq "a_i" dots) (wh pa-false))}]
      :checks [(= (remove even? (range 6)) [1 3 5])]}
     {:name "filterv"
      :cases [{:args [P A] :out (str (vc "a_i" dots) (wh pa-true))}]
      :checks [(= (filterv even? (range 6)) [0 2 4]) (vector? (filterv even? [2]))]}
     {:name "take-while" :lazy true :xf true
      :cases [{:args [P (sq "a_0" dots "a_{i-1}" "a_i" (ul dots))]
               :out (str (sq "a_0" dots "a_{i-1}") (wh (first-wh pa-false)))}]
      :checks [(= (take-while #(< % 3) (range 10)) [0 1 2])
               (= #{0 1 2 3} (realized-by #(doall (take-while (fn [x] (< x 3)) %))))]}
     {:name "drop-while" :lazy true :xf true
      :cases [{:args [P (sq "a_0" dots "a_{i-1}" "a_i" dots)]
               :out (str (sq "a_i" dots) (wh (first-wh pa-false)))}]
      :checks [(= (drop-while #(< % 3) (range 5)) [3 4])]}
     {:name "distinct" :lazy true :xf true
      :cases [{:args [A] :out (str (sq "a_i" dots) (wh "a_i \\ne a_j " (t " for every ") "j < i"))}]
      :checks [(= (distinct [1 2 1 3 2]) [1 2 3]) (lazy-on-infinite? distinct)]}
     {:name "dedupe" :lazy true :xf true
      :cases [{:args [A] :out (str (sq "a_0" "a_i" dots) (wh "a_i \\ne a_{i-1}"))}]
      :checks [(= (dedupe [1 1 2 2 1]) [1 2 1]) (lazy-on-infinite? dedupe)]}
     {:name "random-sample" :lazy true :xf true
      :cases [{:args ["q" A] :out (str (sq "a_i" dots) (wh (t "each ") "a_i" (t " is kept with probability ") "q"))}]
      :checks [(= (random-sample 1.0 [1 2]) [1 2]) (= (random-sample 0.0 [1 2]) [])]}]}

   {:id "transforming"
    :rows
    [{:name "map" :lazy true :xf true
      :cases [{:args [F A] :out (sq (ap "f" "a_0") (ap "f" "a_1") dots)}
              {:args ["f : x\\ y \\to z" (sq "a_0" dots "a_i") (sq "b_0" dots "b_i" "b_{i+1}" (ul "b_{i+2}" dots))]
               :out (sq (ap "f" "a_0" "b_0") dots (ap "f" "a_i" "b_i"))}]
      :checks [(= (map inc [1 2 3]) [2 3 4])
               (= (map + [1 2] [10 20 30]) [11 22])
               (= #{0 1 2} (realized-by #(doall (map + [10 20] %))))]}
     {:name "mapv"
      :cases [{:args [F A] :out (vc (ap "f" "a_0") (ap "f" "a_1") dots)}]
      :checks [(= (mapv inc [1 2]) [2 3]) (vector? (mapv inc [1]))]}
     {:name "pmap" :lazy true
      :cases [{:args [F A] :out (str (sq (ap "f" "a_0") (ap "f" "a_1") dots) (wh (ap "f" "a_i") (t " runs in parallel")))}]
      :checks [(= (pmap inc [1 2 3]) [2 3 4])]}
     {:name "map-indexed" :lazy true :xf true
      :cases [{:args ["f : i\\ x \\to y" A] :out (sq (ap "f" "0" "a_0") (ap "f" "1" "a_1") dots)}]
      :checks [(= (map-indexed vector [:a :b]) [[0 :a] [1 :b]])]}
     {:name "keep" :lazy true :xf true
      :cases [{:args [F A] :out (str (sq (ap "f" "a_i") dots) (wh (ap "f" "a_i") " \\ne " nil'))}]
      :checks [(= (keep #(when (odd? %) (* 10 %)) [1 2 3]) [10 30]) (= (keep identity [false nil 1]) [false 1])]}
     {:name "keep-indexed" :lazy true :xf true
      :cases [{:args ["f : i\\ x \\to y" A] :out (str (sq (ap "f" "i" "a_i") dots) (wh (ap "f" "i" "a_i") " \\ne " nil'))}]
      :checks [(= (keep-indexed #(when (even? %1) %2) [:a :b :c]) [:a :c])]}
     {:name "mapcat" :lazy true :xf true
      :cases [{:args [(str "f : x \\to " (sq "y" dots)) A]
               :out (sq (ub (sp "y_0" "y_1" dots) (ap "f" "a_0"))
                        (ub (sp "z_0" "z_1" dots) (ap "f" "a_1"))
                        dots)}]
      :checks [(= (mapcat #(repeat % %) [1 2 3]) [1 2 2 3 3 3])]}
     {:name "replace" :lazy true :xf true
      :cases [{:args [(mp (sp "k_0" "v_0") dots) A] :out (str (sq "r_0" "r_1" dots) (wh "r_i = " (ap "get" "smap" "a_i" "a_i")))}
              {:args [(mp (sp "k_0" "v_0") dots) (vc "a_0" "a_1" dots)] :out (vc "r_0" "r_1" dots)}]
      :checks [(= (replace {1 :one} '(1 2)) [:one 2]) (= (replace {1 :one} [1 2]) [:one 2]) (vector? (replace {1 :one} [1 2]))]}
     {:name "reductions" :lazy true
      :cases [{:args [F2 A] :out (sq "a_0" (ap "f" "a_0" "a_1") (ap "f" (ap "f" "a_0" "a_1") "a_2") dots)}
              {:args [F2 "\\mathit{init}" A] :out (sq "\\mathit{init}" (ap "f" "\\mathit{init}" "a_0") (ap "f" (ap "f" "\\mathit{init}" "a_0") "a_1") dots)}]
      :checks [(= (reductions + [1 2 3]) [1 3 6]) (= (reductions + 10 [1 2]) [10 11 13])]}]}

   {:id "growing"
    :rows
    [{:name "cons"
      :cases [{:args ["x" A] :out (sq "x" "a_0" "a_1" dots)}]
      :checks [(= (cons 0 [1 2]) [0 1 2]) (= #{} (realized-by #(do (cons :x %) nil)))]}
     {:name "concat" :lazy true
      :cases [{:args [An Bm dots] :out (sq "a_0" dots "a_{n-1}" "b_0" dots "b_{m-1}" dots)}]
      :checks [(= (concat [1 2] [3] [4]) [1 2 3 4])]}
     {:name "interleave" :lazy true
      :cases [{:args [(sq "a_0" dots "a_i" "a_{i+1}" (ul "a_{i+2}" dots)) (sq "b_0" dots "b_i")]
               :out (sq "a_0" "b_0" dots "a_i" "b_i")}]
      :checks [(= (interleave [1 2 3] [:a :b]) [1 :a 2 :b])
               (= #{0 1 2} (realized-by #(doall (interleave % [:a :b]))))]}
     {:name "interpose" :lazy true :xf true
      :cases [{:args ["s" An] :out (sq "a_0" "s" "a_1" "s" dots "s" "a_{n-1}")}]
      :checks [(= (interpose 0 [1 2 3]) [1 0 2 0 3])]}
     {:name "cycle" :lazy true
      :cases [{:args [An] :out (sq "a_0" dots "a_{n-1}" "a_0" dots "a_{n-1}" dots)}]
      :checks [(= (take 5 (cycle [1 2])) [1 2 1 2 1])]}
     {:name "flatten" :lazy true
      :cases [{:args [(sq "a_0" (sq "b_0" (sq "c_0" "c_1")) "d_0")] :out (sq "a_0" "b_0" "c_0" "c_1" "d_0")}]
      :checks [(= (flatten [1 [2 [3 4]] 5]) [1 2 3 4 5])]}]}

   {:id "reordering"
    :rows
    [{:name "reverse"
      :cases [{:args [An] :out (sq "a_{n-1}" dots "a_0")}]
      :checks [(= (reverse [1 2 3]) [3 2 1])]}
     {:name "sort"
      :cases [{:args [A] :out (str (sq "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh "a_{\\sigma(0)} \\le a_{\\sigma(1)} \\le \\ldots"))}
              {:args [(str "c : x\\ x \\to " (t "int")) A] :out (str (sq "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh (t "ordered by ") "c"))}]
      :checks [(= (sort [3 1 2]) [1 2 3]) (= (sort > [3 1 2]) [3 2 1])]}
     {:name "sort-by"
      :cases [{:args ["k : x \\to y" A] :out (str (sq "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh (ap "k" "a_{\\sigma(0)}") " \\le " (ap "k" "a_{\\sigma(1)}") " \\le \\ldots"))}]
      :checks [(= (sort-by count ["bb" "a" "ccc"]) ["a" "bb" "ccc"]) (= (sort-by first [[1 :b] [0 :x] [1 :a]]) [[0 :x] [1 :b] [1 :a]])]}
     {:name "shuffle"
      :cases [{:args [A] :out (str (vc "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh "\\sigma" (t " is a random permutation")))}]
      :checks [(= (sort (shuffle [3 1 2])) [1 2 3]) (vector? (shuffle [1]))]}]}

   {:id "splitting"
    :rows
    [{:name "partition" :lazy true
      :cases [{:args ["k" A] :out (str (sq (sq "a_0" dots "a_{k-1}") (sq "a_k" dots "a_{2k-1}") dots) (wh (t "a short last group is dropped")))}
              {:args ["k" "s" A] :out (sq (sq "a_0" dots "a_{k-1}") (sq "a_s" dots "a_{s+k-1}") dots)}]
      :checks [(= (partition 2 [0 1 2 3 4]) [[0 1] [2 3]]) (= (partition 2 1 [0 1 2]) [[0 1] [1 2]])]}
     {:name "partition-all" :lazy true :xf true
      :cases [{:args ["k" A] :out (str (sq (sq "a_0" dots "a_{k-1}") (sq "a_k" dots "a_{2k-1}") dots) (wh (t "the last group may be short")))}]
      :checks [(= (partition-all 2 [0 1 2 3 4]) [[0 1] [2 3] [4]])]}
     {:name "partition-by" :lazy true :xf true
      :cases [{:args [F (sq "a_0" dots "a_{i-1}" "a_i" dots)]
               :out (str (sq (sq "a_0" dots "a_{i-1}") (sq "a_i" dots) dots) (wh (ap "f" "a_{i-1}") " \\ne " (ap "f" "a_i")))}]
      :checks [(= (partition-by odd? [1 3 2 4 5]) [[1 3] [2 4] [5]])]}
     {:name "split-at"
      :cases [{:args ["k" (sq "a_0" dots "a_{k-1}" "a_k" dots)] :out (vc (sq "a_0" dots "a_{k-1}") (sq "a_k" dots))}]
      :checks [(= (split-at 2 [1 2 3]) [[1 2] [3]]) (vector? (split-at 1 [1]))]}
     {:name "split-with"
      :cases [{:args [P (sq "a_0" dots "a_{i-1}" "a_i" dots)]
               :out (str (vc (sq "a_0" dots "a_{i-1}") (sq "a_i" dots)) (wh (first-wh pa-false)))}]
      :checks [(= (split-with odd? [1 3 2 5]) [[1 3] [2 5]])]}
     {:name "partitionv" :lazy true
      :cases [{:args ["k" A] :out (str (sq (vc "a_0" dots "a_{k-1}") (vc "a_k" dots "a_{2k-1}") dots) (wh (t "a short last group is dropped")))}]
      :checks [(= (partitionv 2 [0 1 2]) [[0 1]]) (vector? (first (partitionv 2 [0 1])))]}
     {:name "partitionv-all" :lazy true :xf true
      :cases [{:args ["k" A] :out (str (sq (vc "a_0" dots "a_{k-1}") (vc "a_k" dots "a_{2k-1}") dots) (wh (t "the last group may be short")))}]
      :checks [(= (partitionv-all 2 [0 1 2]) [[0 1] [2]]) (vector? (first (partitionv-all 2 [0])))]}
     {:name "splitv-at"
      :cases [{:args ["k" (sq "a_0" dots "a_{k-1}" "a_k" dots)] :out (vc (vc "a_0" dots "a_{k-1}") (sq "a_k" dots))}]
      :checks [(= (splitv-at 2 [1 2 3]) [[1 2] [3]]) (vector? (first (splitv-at 1 [1 2])))]}]}

   {:id "reducing"
    :rows
    [{:name "reduce"
      :cases [{:args [F2 A] :out (ap "f" (ap "f" (ap "f" "a_0" "a_1") "a_2") dots)}
              {:args [F2 (sq "a_0")] :out "a_0"}
              {:args [F2 (sq)] :out "(f)"}
              {:args [F2 "\\mathit{init}" A] :out (ap "f" (ap "f" (ap "f" "\\mathit{init}" "a_0") "a_1") dots)}
              {:args [F2 "\\mathit{init}" (sq)] :out "\\mathit{init}"}]
      :checks [(= (reduce - [10 1 2]) 7) (= (reduce + [5]) 5) (= (reduce + []) 0) (= (reduce - 10 [1 2]) 7) (= (reduce + 3 []) 3)]}
     {:name "reduce-kv"
      :cases [{:args ["f : \\mathit{acc}\\ k\\ v \\to \\mathit{acc}" "\\mathit{init}" (mp (sp "k_0" "v_0") (sp "k_1" "v_1") dots)]
               :out (ap "f" (ap "f" "\\mathit{init}" "k_0" "v_0") "k_1" "v_1" dots)}]
      :checks [(= (reduce-kv #(+ %1 %2 %3) 0 {1 10 2 20}) 33)]}
     {:name "transduce"
      :cases [{:args ["\\mathit{xf}" F2 "\\mathit{init}" A]
               :out (str (ap "f" (ap "f" (ap "f" "\\mathit{init}" "b_0") "b_1") dots) (wh (sq "b_0" "b_1" dots) " = " (ap "sequence" "\\mathit{xf}" "a")))}]
      :checks [(= (transduce (map inc) + 0 [1 2]) 5)]}
     {:name "count"
      :cases [{:args [An] :out "n"} {:args [nil'] :out "0"}]
      :checks [(= (count [1 2 3]) 3) (= (count nil) 0)]}
     {:name "first"
      :cases [{:args [(sq "a_0" (ul "a_1" dots))] :out "a_0"} {:args [(sq)] :out nil'}]
      :checks [(= (first [1 2]) 1) (nil? (first [])) (= #{0} (realized-by first))]}
     {:name "second"
      :cases [{:args [(sq "a_0" "a_1" (ul "a_2" dots))] :out "a_1"} {:args [(sq "a_0")] :out nil'}]
      :checks [(= (second [1 2 3]) 2) (nil? (second [1])) (= #{0 1} (realized-by second))]}
     {:name "last"
      :cases [{:args [An] :out "a_{n-1}"} {:args [(sq)] :out nil'}]
      :checks [(= (last [1 2 3]) 3) (nil? (last []))]}
     {:name "nth"
      :cases [{:args [(sq "a_0" dots "a_i" (ul dots)) "i"] :out "a_i"}
              {:args [An "i \\ge n"] :out (t "throws")}
              {:args [An "i \\ge n" "d"] :out "d"}]
      :checks [(= (nth [1 2 3] 1) 2) (= :d (nth [1] 5 :d))
               (= :thrown (try (nth [1] 5) (catch IndexOutOfBoundsException _ :thrown)))
               (= #{0 1 2} (realized-by #(nth % 2)))]}
     {:name "ffirst"
      :cases [{:args [(sq (sq "b_0" dots) dots)] :out "b_0"}]
      :checks [(= (ffirst [[1 2] [3]]) 1)]}
     {:name "rand-nth"
      :cases [{:args [An] :out (str "a_i" (wh "i" (t " is random")))}]
      :checks [(contains? #{1 2} (rand-nth [1 2]))]}
     {:name "some"
      :cases [{:args [F (sq "a_0" dots "a_i" (ul dots))]
               :out (cases (ap "f" "a_i") (str (t "wh. ") (first-wh (ap "f" "a_i") (t " truthy")))
                           nil' (t "otherwise"))}]
      :checks [(= (some #(when (> % 1) (* 10 %)) [1 2 3]) 20) (nil? (some even? [1 3]))
               (= #{0 1 2} (realized-by #(some (fn [x] (when (= x 2) x)) %)))]}
     {:name "every?"
      :cases [{:args [P (sq "a_0" dots "a_i" (ul dots))]
               :out (cases false' (str (t "wh. ") (first-wh pa-false))
                           true' (t "otherwise"))}]
      :checks [(true? (every? odd? [1 3])) (false? (every? odd? [1 2 3])) (true? (every? odd? []))
               (= #{0 1} (realized-by #(every? (fn [x] (< x 1)) %)))]}
     {:name "not-every?"
      :cases [{:args [P (sq "a_0" dots "a_i" (ul dots))]
               :out (cases true' (str (t "wh. ") (first-wh pa-false))
                           false' (t "otherwise"))}]
      :checks [(true? (not-every? odd? [1 2])) (false? (not-every? odd? [1 3]))]}
     {:name "not-any?"
      :cases [{:args [P (sq "a_0" dots "a_i" (ul dots))]
               :out (cases false' (str (t "wh. ") (first-wh pa-true))
                           true' (t "otherwise"))}]
      :checks [(true? (not-any? even? [1 3])) (false? (not-any? even? [1 2]))]}
     {:name "empty?"
      :cases [{:args [(sq)] :out true'} {:args [(sq "a_0" (ul dots))] :out false'}]
      :checks [(true? (empty? [])) (false? (empty? [1])) (= #{0} (realized-by empty?))]}
     {:name "not-empty"
      :cases [{:args [(sq)] :out nil'} {:args ["c"] :out "c"}]
      :checks [(nil? (not-empty [])) (= [1] (not-empty [1]))]}
     {:name "max-key"
      :cases [{:args [(str "k : x \\to " (t "number")) "x_0" "x_1" dots] :out (str "x_i" (wh (ap "k" "x_i") (t " is the largest (the last one on a tie)")))}]
      :checks [(= "ccc" (max-key count "a" "ccc" "bb")) (= "cd" (max-key count "ab" "cd"))]}
     {:name "min-key"
      :cases [{:args [(str "k : x \\to " (t "number")) "x_0" "x_1" dots] :out (str "x_i" (wh (ap "k" "x_i") (t " is the smallest (the last one on a tie)")))}]
      :checks [(= "a" (min-key count "a" "ccc" "bb")) (= "cd" (min-key count "ab" "cd"))]}]}

   {:id "collecting"
    :rows
    [{:name "into"
      :cases [{:args [(vc "t_0" dots) A] :out (vc "t_0" dots "a_0" "a_1" dots)}
              {:args [(sq "t_0" dots) A] :out (sq dots "a_1" "a_0" "t_0" dots)}
              {:args [(mp (sp "k_0" "v_0") dots) (sq (vc "k" "v") dots)] :out (mp (sp "k_0" "v_0") dots (sp "k" "v") dots)}
              {:args ["c" "\\mathit{xf}" A] :out (ap "into" "c" (ap "sequence" "\\mathit{xf}" "a"))}]
      :checks [(= (into [0] [1 2]) [0 1 2]) (= (into '(0) [1 2]) '(2 1 0)) (= (into {:a 1} [[:b 2]]) {:a 1 :b 2})
               (= (into [] (map inc) [1 2]) [2 3])]}
     {:name "vec"
      :cases [{:args [A] :out (vc "a_0" "a_1" dots)}]
      :checks [(= (vec '(1 2)) [1 2]) (vector? (vec nil))]}
     {:name "set"
      :cases [{:args [A] :out (st "a_0" "a_1" dots)}]
      :checks [(= (set [1 2 1]) #{1 2})]}
     {:name "sequence" :lazy true
      :cases [{:args [A] :out A}
              {:args [(sq)] :out (sq)}
              {:args ["\\mathit{xf}" A] :out (str (sq "b_0" "b_1" dots) (wh (t "the ") "b_i" (t " are what ") "\\mathit{xf}" (t " passes on")))}]
      :checks [(= (sequence [1 2]) [1 2]) (= () (sequence [])) (= (sequence (filter odd?) [1 2 3]) [1 3])]}
     {:name "zipmap"
      :cases [{:args [(sq "k_0" dots "k_i") (sq "v_0" dots "v_i" "v_{i+1}" (ul "v_{i+2}" dots))] :out (mp (sp "k_0" "v_0") dots (sp "k_i" "v_i"))}]
      :checks [(= (zipmap [:a :b] [1 2 3]) {:a 1 :b 2})
               (= #{0 1 2} (realized-by #(zipmap [:a :b] %)))]}
     {:name "frequencies"
      :cases [{:args [A] :out (str (mp (sp "a_i" "c_i") dots) (wh "c_i" (t " is how many times ") "a_i" (t " occurs")))}]
      :checks [(= (frequencies [:a :b :a]) {:a 2 :b 1})]}
     {:name "group-by"
      :cases [{:args [F A] :out (str (mp (sp "y" (vc "a_i" dots)) dots) (wh (ap "f" "a_i") " = y"))}]
      :checks [(= (group-by odd? [1 2 3]) {true [1 3] false [2]})]}]}

   {:id "collections"
    :rows
    [{:name "conj"
      :cases [{:args [(vc "a_0" dots) "x"] :out (vc "a_0" dots "x")}
              {:args [(sq "a_0" dots) "x"] :out (sq "x" "a_0" dots)}
              {:args [(st "a_0" dots) "x"] :out (st "a_0" dots "x")}
              {:args [(mp (sp "k_0" "v_0") dots) (vc "k" "v")] :out (mp (sp "k_0" "v_0") dots (sp "k" "v"))}
              {:args [nil' "x"] :out (sq "x")}]
      :checks [(= (conj [1] 2) [1 2]) (= (conj '(1) 2) '(2 1)) (= (conj #{1} 2) #{1 2}) (= (conj {:a 1} [:b 2]) {:a 1 :b 2}) (= (conj nil 1) '(1))]}
     {:name "disj"
      :cases [{:args [(st "a_0" dots "x" dots) "x"] :out (st "a_0" dots)}]
      :checks [(= (disj #{1 2} 2) #{1})]}
     {:name "assoc"
      :cases [{:args [(mp (sp "k_0" "v_0") dots) "k" "v"] :out (str (mp (sp "k_0" "v_0") dots (sp "k" "v")) (wh (t "a previous value of ") "k" (t " is replaced")))}
              {:args [(vc "a_0" dots "a_i" dots) "i" "x"] :out (vc "a_0" dots "x" dots)}]
      :checks [(= (assoc {:a 1} :b 2) {:a 1 :b 2}) (= (assoc {:a 1} :a 2) {:a 2}) (= (assoc [1 2] 1 :x) [1 :x]) (= (assoc [1] 1 :x) [1 :x])]}
     {:name "dissoc"
      :cases [{:args [(mp (sp "k_0" "v_0") dots (sp "k" "v")) "k"] :out (mp (sp "k_0" "v_0") dots)}]
      :checks [(= (dissoc {:a 1 :b 2} :b) {:a 1})]}
     {:name "get"
      :cases [{:args [(mp dots (sp "k" "v") dots) "k"] :out "v"}
              {:args ["m" "k"] :out (str nil' (wh "k" (t " is not a key of ") "m"))}
              {:args ["m" "k" "d"] :out (str "d" (wh "k" (t " is not a key of ") "m"))}
              {:args [(vc "a_0" dots "a_i" dots) "i"] :out "a_i"}]
      :checks [(= (get {:a 1} :a) 1) (nil? (get {} :a)) (= :d (get {} :a :d)) (= 2 (get [1 2] 1))]}
     {:name "get-in"
      :cases [{:args ["m" (vc "k_0" "k_1" dots)] :out (ap "get" (ap "get" "m" "k_0") "k_1" dots)}]
      :checks [(= (get-in {:a {:b 1}} [:a :b]) 1) (nil? (get-in {:a 1} [:b :c]))]}
     {:name "assoc-in"
      :cases [{:args ["m" (vc "k_0" "k_1") "v"] :out (ap "assoc" "m" "k_0" (ap "assoc" (ap "get" "m" "k_0") "k_1" "v"))}]
      :checks [(= (assoc-in {} [:a :b] 1) {:a {:b 1}})]}
     {:name "update"
      :cases [{:args [(mp dots (sp "k" "v") dots) "k" F] :out (mp dots (sp "k" (ap "f" "v")) dots)}
              {:args [(mp dots (sp "k" "v") dots) "k" "f" "x_0" dots] :out (mp dots (sp "k" (ap "f" "v" "x_0" dots)) dots)}]
      :checks [(= (update {:a 1} :a inc) {:a 2}) (= (update {:a 1} :a + 10) {:a 11}) (= (update {} :a (fnil inc 0)) {:a 1})]}
     {:name "update-in"
      :cases [{:args ["m" (vc "k_0" "k_1") F] :out (ap "assoc-in" "m" (vc "k_0" "k_1") (ap "f" (ap "get-in" "m" (vc "k_0" "k_1"))))}]
      :checks [(= (update-in {:a {:b 1}} [:a :b] inc) {:a {:b 2}})]}
     {:name "update-keys"
      :cases [{:args [(mp (sp "k_0" "v_0") dots) "f : k \\to k"] :out (mp (sp (ap "f" "k_0") "v_0") dots)}]
      :checks [(= (update-keys {:a 1} name) {"a" 1})]}
     {:name "update-vals"
      :cases [{:args [(mp (sp "k_0" "v_0") dots) "f : v \\to v"] :out (mp (sp "k_0" (ap "f" "v_0")) dots)}]
      :checks [(= (update-vals {:a 1} inc) {:a 2})]}
     {:name "merge"
      :cases [{:args [(mp (sp "k_0" "v_0") dots) (mp (sp "k_0" "w_0") (sp "j_0" "u_0") dots) dots]
               :out (str (mp (sp "k_0" "w_0") dots (sp "j_0" "u_0") dots) (wh (t "the later map wins")))}]
      :checks [(= (merge {:a 1 :b 2} {:a 3 :c 4}) {:a 3 :b 2 :c 4}) (nil? (merge))]}
     {:name "merge-with"
      :cases [{:args ["f : v\\ v \\to v" (mp (sp "k_0" "v_0") dots) (mp (sp "k_0" "w_0") (sp "j_0" "u_0") dots)]
               :out (mp (sp "k_0" (ap "f" "v_0" "w_0")) dots (sp "j_0" "u_0") dots)}]
      :checks [(= (merge-with + {:a 1 :b 2} {:a 3 :c 4}) {:a 4 :b 2 :c 4})]}
     {:name "select-keys"
      :cases [{:args ["m" (vc "k_0" "k_1" dots)] :out (str (mp (sp "k_i" "v_i") dots) (wh "k_i" (t " is a key of ") "m"))}]
      :checks [(= (select-keys {:a 1 :b 2} [:a :z]) {:a 1})]}
     {:name "contains?"
      :cases [{:args ["m" "k"] :out (cases true' (str (t "wh. ") "k" (t " is a key of ") "m") false' (t "otherwise"))}
              {:args [(vc "a_0" dots "a_{n-1}") "i"] :out (cases true' (str (t "wh. ") "0 \\le i < n") false' (t "otherwise"))}]
      :checks [(true? (contains? {:a nil} :a)) (false? (contains? {} :a)) (true? (contains? [:x :y] 1)) (false? (contains? [:x :y] :x))]}
     {:name "find"
      :cases [{:args [(mp dots (sp "k" "v") dots) "k"] :out (vc "k" "v")}
              {:args ["m" "k"] :out (str nil' (wh "k" (t " is not a key of ") "m"))}]
      :checks [(= (find {:a 1} :a) [:a 1]) (map-entry? (find {:a 1} :a)) (nil? (find {} :a))]}
     {:name "peek"
      :cases [{:args [(vc "a_0" dots "a_{n-1}")] :out "a_{n-1}"}
              {:args [(sq "a_0" dots)] :out "a_0"}]
      :checks [(= (peek [1 2 3]) 3) (= (peek '(1 2 3)) 1)]}
     {:name "pop"
      :cases [{:args [(vc "a_0" dots "a_{n-1}")] :out (vc "a_0" dots "a_{n-2}")}
              {:args [(sq "a_0" "a_1" dots)] :out (sq "a_1" dots)}
              {:args [(vc)] :out (t "throws")}]
      :checks [(= (pop [1 2 3]) [1 2]) (= (pop '(1 2 3)) '(2 3))
               (= :thrown (try (pop []) (catch IllegalStateException _ :thrown)))]}
     {:name "subvec"
      :cases [{:args [(vc "a_0" dots "a_{n-1}") "i" "j"] :out (vc "a_i" dots "a_{j-1}")}
              {:args [(vc "a_0" dots "a_{n-1}") "i"] :out (vc "a_i" dots "a_{n-1}")}]
      :checks [(= (subvec [0 1 2 3] 1 3) [1 2]) (= (subvec [0 1 2] 1) [1 2])]}
     {:name "empty"
      :cases [{:args [(vc "a_0" dots)] :out (vc)}
              {:args [(mp (sp "k_0" "v_0") dots)] :out (mp)}]
      :checks [(= (empty [1 2]) []) (vector? (empty [1])) (= (empty {:a 1}) {}) (map? (empty {:a 1}))]}]}

   {:id "functions"
    :rows
    [{:name "identity"
      :cases [{:args ["x"] :out "x"}]
      :checks [(= (identity 1) 1)]}
     {:name "constantly"
      :cases [{:args ["x"] :out (lam (sq "y_0" dots) "x")}]
      :checks [(= ((constantly 1) :a :b) 1)]}
     {:name "comp"
      :cases [{:args ["f" "g" "h"] :out (lam (sq "x_0" dots) (ap "f" (ap "g" (ap "h" "x_0" dots))))}
              {:args [] :out (lam "x" "x")}]
      :checks [(= ((comp inc *) 2 3) 7) (= ((comp) 5) 5)]}
     {:name "partial"
      :cases [{:args ["f" "x_0" dots] :out (lam (sq "y_0" dots) (ap "f" "x_0" dots "y_0" dots))}]
      :checks [(= ((partial - 10) 1 2) 7)]}
     {:name "apply"
      :cases [{:args ["f" "x_0" dots A] :out (ap "f" "x_0" dots "a_0" "a_1" dots)}]
      :checks [(= (apply + 1 [2 3]) 6)]}
     {:name "complement"
      :cases [{:args [P] :out (lam "x" (ap "not" (ap "p" "x")))}]
      :checks [(= ((complement even?) 1) true)]}
     {:name "juxt"
      :cases [{:args ["f" "g" dots] :out (lam "x" (vc (ap "f" "x") (ap "g" "x") dots))}]
      :checks [(= ((juxt inc dec) 1) [2 0])]}
     {:name "fnil"
      :cases [{:args ["f" "d"] :out (lam (sq "x" dots) (str (ap "f" "x'" dots) (wh "x' = d" (t " if ") "x = " nil' (t ", else ") "x' = x")))}]
      :checks [(= ((fnil inc 0) nil) 1) (= ((fnil inc 0) 5) 6) (= ((fnil not 0) false) true)]}
     {:name "every-pred"
      :cases [{:args ["p" "q" dots] :out (lam "x" (ap "boolean" (ap "and" (ap "p" "x") (ap "q" "x") dots)))}]
      :checks [(true? ((every-pred odd? pos?) 1)) (false? ((every-pred odd? pos?) -1))]}
     {:name "some-fn"
      :cases [{:args ["f" "g" dots] :out (lam "x" (ap "or" (ap "f" "x") (ap "g" "x") dots))}]
      :checks [(= ((some-fn :a :b) {:b 2}) 2) (nil? ((some-fn :a :b) {}))]}
     {:name "memoize"
      :cases [{:args [F] :out (str "f'" (wh (ap "f'" "x") (t " calls ") (ap "f" "x") (t " once per distinct ") "x"))}]
      :checks [(let [n (atom 0) g (memoize (fn [x] (swap! n inc) x))] (g 1) (g 1) (g 2) (= 2 @n))]}]}])

(defn math [s]
  (str "<span class=\"math\" data-tex=\""
       (.encodeToString (Base64/getEncoder) (.getBytes ^String s "UTF-8"))
       "\"></span>"))

(defn doc-url [name]
  (str "https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/" name))

(defn escape [s] (str/escape s {\< "&lt;" \> "&gt;" \& "&amp;" \" "&quot;"}))

(defn row-html [{:keys [name lazy xf cases]}]
  (let [n (count cases)
        span (fn [body] (str "<td" (when (> n 1) (str " rowspan=\"" n "\"")) ">" body "</td>"))]
    (str/join
     "\n"
     (map-indexed
      (fn [i {:keys [args out]}]
        (str "<tr>"
             (when (zero? i)
               (span (str "<a href=\"" (doc-url name) "\"><code style=\"white-space: nowrap\">" (escape name) "</code></a>")))
             "<td>" (if (seq args) (str/join ", " (map math args)) "") "</td>"
             "<td>" (math out) "</td>"
             (when (zero? i)
               (str (span (if lazy "✓" "")) (span (if xf "✓" ""))))
             "</tr>"))
      cases))))

(defn table-html [{:keys [rows]}]
  (str "<table>\n<thead><tr><th>Function</th><th>Arguments</th><th>Result</th><th>Lazy</th><th>xf</th></tr></thead>\n<tbody>\n"
       (str/join "\n" (map row-html rows))
       "\n</tbody>\n</table>\n"))

(defmacro checked-sections []
  `[~@(for [{:keys [id rows]} sections]
        `{:id ~id
          :results [~@(for [{:keys [name checks]} rows]
                        `[~name [~@(for [c checks] `[(quote ~c) (boolean ~c)])]])]})])

(defn transducer-arity?
  "True when the arglists have [args...] next to [args... coll] or
  [args... & colls], the shape of a function whose collection-less arity
  returns a transducer. conj has that shape for its reducing-function arities."
  [name]
  (let [arglists (set (map vec (:arglists (meta (ns-resolve 'clojure.core (symbol name))))))
        coll-arg? (fn [sym] (and (symbol? sym) (re-matches #"coll|c\d+" (clojure.core/name sym))))]
    (and (not= name "conj")
         (boolean (some (fn [args]
                          (and (not-any? coll-arg? args)
                               (or (contains? arglists (conj args 'coll))
                                   (contains? arglists (conj args '& 'colls)))))
                        arglists)))))

(defn -main [out-dir]
  (let [rows (mapcat :rows sections)
        failures (for [{:keys [results]} (checked-sections)
                       [name cs] results
                       [form ok] cs
                       :when (not ok)]
                   [name form])
        missing (for [{:keys [name]} rows
                      :when (not (contains? (ns-publics 'clojure.core) (symbol name)))]
                  name)
        dupes (for [[name n] (frequencies (map :name rows)) :when (> n 1)] name)
        xf-mismatch (for [{:keys [name xf]} rows
                          :when (not= (boolean xf) (transducer-arity? name))]
                      name)]
    (doseq [[name form] failures] (println "FAIL" name (pr-str form)))
    (doseq [name missing] (println "NOT IN clojure.core" name))
    (doseq [name dupes] (println "DUPLICATE" name))
    (doseq [name xf-mismatch] (println "XF MISMATCH" name))
    (when (or (seq failures) (seq missing) (seq dupes) (seq xf-mismatch)) (System/exit 1))
    (io/make-parents (io/file out-dir "x"))
    (doseq [{:keys [id] :as section} sections]
      (spit (io/file out-dir (str id ".html")) (table-html section)))
    (println "ok" (clojure-version)
             (count rows) "functions"
             (count (mapcat :checks rows)) "checks")))

(apply -main *command-line-args*)
