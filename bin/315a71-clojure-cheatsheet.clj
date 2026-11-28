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
(defn t
  ([s] (t s s))
  ([en ja] (str "\u0001" en "\u0002" ja "\u0003")))
(defn kw [s] (str "\\mathtt{" s "}"))
(def dots "\\ldots")
(def nil' (kw "nil"))
(def true' (kw "true"))
(def false' (kw "false"))
(defn wh [& xs] (str "\\quad " (t "wh. " "ただし ") (apply str xs)))
(defn lam [args body] (str args " \\mapsto " body))
(defn cases [& rows]
  (str "\\begin{array}{ll}"
       (str/join " \\\\ " (map (fn [[v c]] (str v " & " c)) (partition 2 rows)))
       "\\end{array}"))
(defn first-wh [& cond'] (str "i" (t " is the first index with " " は ") (apply str cond') (t "" " となる最初の位置")))

(def A (sq "a_0" "a_1" dots))
(def Al (sq "a_0" dots "a_{l-1}"))
(def Bl (sq "b_0" dots "b_{l'-1}"))
(defn ls [& xs] (str "\\texttt{'}" (apply sq xs)))
(def P (str "p : x \\to " (t "bool")))
(def F "f : x \\to y")
(def F2 "f : \\mathit{acc}\\ x \\to \\mathit{acc}")
(def kf "\\mathit{keyfn}")
(def G (str kf " : x \\to y"))
(defn ap [head & xs]
  (apply sq (if (re-matches #"[a-z][a-z-]+" head)
              (str "\\mathtt{" (str/replace head "-" "\\text{-}") "}")
              head)
         xs))
(def pa-false (str (ap "p" "a_i") " = " false'))
(def pa-true (str (ap "p" "a_i") " = " true'))
(def M (mp (sp "k_0" "v_0") (sp "k_1" "v_1") dots))
(def init "\\mathit{init}")
(def xf "\\mathit{xf}")

(def samples
  {:list '(1) :vector [1] :seq (seq [1 2]) :lazy (map inc [1]) :map {:a 1} :set #{1}
   :entry (first {:a 1}) :sorted-map (sorted-map :a 1) :sorted-set (sorted-set 1)
   :string "a" :nil nil :array (int-array [1])})

(defn true-for [pred]
  (set (filter #(pred (samples %)) (keys samples))))

(defn tr [& xs] (str (apply vc xs) "^{!}"))
(defn tm [& xs] (str (apply mp xs) "^{!}"))
(defn ts [& xs] (str (apply st xs) "^{!}"))

(defn pred-row [name kinds kinds-ja checks]
  {:name name
   :cases [{:args ["x"] :out (cases true' (str (t "wh. " "") "x" (t (str " is " kinds) (str " が" kinds-ja "のとき"))) false' (t "otherwise" "それ以外のとき"))}]
   :checks checks})

(def sections
  [{:id "making"
    :rows
    [{:name "seq"
      :cases [{:args [(vc "a_0" "a_1" dots)] :out A}
              {:args [(vc)] :out nil'}]
      :checks [(= (seq [1 2]) '(1 2)) (nil? (seq [])) (nil? (seq nil))]}
     {:name "sequence"
      :cases [{:args [(vc "a_0" "a_1" dots)] :out A}
              {:args [(vc)] :out (sq)}
              {:args [xf A] :lazy true :out (str (sq "b_0" "b_1" dots) (wh (t "the " "") "b_i" (t " are what " " は ") xf (t " passes on" " が渡す値")))}]
      :checks [(= (sequence [1 2]) [1 2]) (= () (sequence [])) (= (sequence (filter odd?) [1 2 3]) [1 3])
               (instance? clojure.lang.LazySeq (sequence (map inc) [1]))]}
     {:name "list*"
      :cases [{:args ["x_0" dots A] :out (sq "x_0" dots "a_0" "a_1" dots)}]
      :checks [(= (list* 1 2 [3 4]) [1 2 3 4]) (= #{} (realized-by #(do (list* :x %) nil)))]}
     {:name "re-seq" :lazy true
      :cases [{:args ["\\mathit{re}" "\\mathit{str}"] :out (str (sq "s_0" "s_1" dots) (wh "s_i" (t " is the i-th match, as " " は ") "\\mathtt{re\\text{-}find}" (t " returns it" " が返すi番目の一致")))}
              {:args ["\\mathit{re}" "\\mathit{str}"] :out (str nil' (wh (t "there is no match" "一致が無いとき")))}]
      :checks [(= (re-seq #"\d" "a1b2") ["1" "2"]) (nil? (re-seq #"\d" "ab")) (= (re-seq #"(\d)(\w)" "1a") [["1a" "1" "a"]])]}
     {:name "subseq"
      :cases [{:args [(str (st "a_0" "a_1" dots) (t " sorted" " (ソート済み)")) "\\mathit{test}" "k"] :lazy false :out (str (sq "a_i" dots) (wh (ap "\\mathit{test}" "a_i" "k") (t ", with " "、") "\\mathit{test}" (t " one of " " は ") "> \\ge" (t "" " のどちらか")))}
              {:args [(str (st "a_0" "a_1" dots) (t " sorted" " (ソート済み)")) "\\mathit{test}" "k"] :lazy true :out (str (sq "a_i" dots) (wh (ap "\\mathit{test}" "a_i" "k") (t ", with " "、") "\\mathit{test}" (t " one of " " は ") "< \\le" (t "" " のどちらか")))}
              {:args [(str (st "a_0" "a_1" dots) (t " sorted" " (ソート済み)")) "\\mathit{test}" "k" "\\mathit{test}'" "k'"] :lazy true :out (str (sq "a_i" dots) (wh (ap "\\mathit{test}" "a_i" "k") (t " and " " かつ ") (ap "\\mathit{test}'" "a_i" "k'")))}]
      :checks [(= (subseq (sorted-set 1 2 3) > 1) [2 3]) (= (subseq (sorted-set 1 2 3 4) > 1 < 4) [2 3]) (= (subseq (sorted-map 1 :a 2 :b) >= 2) [[2 :b]])
               (not (instance? clojure.lang.LazySeq (subseq (sorted-set 1 2) > 0)))
               (instance? clojure.lang.LazySeq (subseq (sorted-set 1 2) < 2))
               (instance? clojure.lang.LazySeq (subseq (sorted-set 1 2 3) > 0 < 3))]}
     {:name "rsubseq"
      :cases [{:args [(str (st "a_0" "a_1" dots) (t " sorted" " (ソート済み)")) "\\mathit{test}" "k"] :lazy false :out (str (sq dots "a_i") (wh (ap "\\mathit{test}" "a_i" "k") (t ", with " "、") "\\mathit{test}" (t " one of " " は ") "< \\le" (t "" " のどちらか")))}
              {:args [(str (st "a_0" "a_1" dots) (t " sorted" " (ソート済み)")) "\\mathit{test}" "k"] :lazy true :out (str (sq dots "a_i") (wh (ap "\\mathit{test}" "a_i" "k") (t ", with " "、") "\\mathit{test}" (t " one of " " は ") "> \\ge" (t "" " のどちらか")))}
              {:args [(str (st "a_0" "a_1" dots) (t " sorted" " (ソート済み)")) "\\mathit{test}" "k" "\\mathit{test}'" "k'"] :lazy true :out (str (sq dots "a_i") (wh (ap "\\mathit{test}" "a_i" "k") (t " and " " かつ ") (ap "\\mathit{test}'" "a_i" "k'")))}]
      :checks [(= (rsubseq (sorted-set 1 2 3) < 3) [2 1]) (= (rsubseq (sorted-set 1 2 3 4) > 1 < 4) [3 2])
               (not (instance? clojure.lang.LazySeq (rsubseq (sorted-set 1 2) < 2)))
               (instance? clojure.lang.LazySeq (rsubseq (sorted-set 1 2) > 0))]}
     {:name "range" :lazy true
      :cases [{:args [] :out (sq "0" "1" "2" dots)}
              {:args ["n"] :out (sq "0" "1" dots "n-1")}
              {:args ["i" "j"] :out (sq "i" "i+1" dots "j-1")}
              {:args ["i" "j" "s"] :out (str (sq "i" "i+s" "i+2s" dots) (wh "s \\ne 0" (t ", and it stops before reaching " " で、") "j" (t "" " に達する前に止まる")))}]
      :checks [(= (take 3 (range)) [0 1 2]) (= (range 3) [0 1 2]) (= (range 2 5) [2 3 4]) (= (range 0 7 3) [0 3 6])
               (= (range 5 0 -2) [5 3 1])]}
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
      :cases [{:args [M] :out (sq "k_0" "k_1" dots)}
              {:args [(mp)] :out nil'}]
      :checks [(= (keys (array-map :a 1 :b 2)) [:a :b]) (nil? (keys {}))]}
     {:name "vals"
      :cases [{:args [M] :out (sq "v_0" "v_1" dots)}
              {:args [(mp)] :out nil'}]
      :checks [(= (vals (array-map :a 1 :b 2)) [1 2]) (nil? (vals {}))]}
     {:name "rseq"
      :cases [{:args [(vc "a_0" dots "a_{l-1}")] :out (sq "a_{l-1}" dots "a_0")}
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
      :cases [{:args ["n" Al] :out (sq "a_{l-n}" dots "a_{l-1}")}
              {:args ["n" (sq)] :out nil'}]
      :checks [(= (take-last 2 [1 2 3]) [2 3]) (nil? (take-last 2 []))]}
     {:name "drop-last" :lazy true
      :cases [{:args [Al] :out (sq "a_0" dots "a_{l-2}")}
              {:args ["n" Al] :out (sq "a_0" dots "a_{l-n-1}")}]
      :checks [(= (drop-last [1 2 3]) [1 2]) (= (drop-last 2 [1 2 3]) [1])]}
     {:name "butlast"
      :cases [{:args [Al] :out (sq "a_0" dots "a_{l-2}")}
              {:args [(sq "a_0")] :out nil'}]
      :checks [(= (butlast [1 2 3]) [1 2]) (nil? (butlast [1]))]}
     {:name "rest"
      :cases [{:args [(sq "a_0" "a_1" dots)] :out (sq "a_1" dots)}
              {:args [(sq "a_0")] :out (sq)}
              {:args [nil'] :out (sq)}]
      :checks [(= (rest [1 2]) [2]) (= (rest [1]) ()) (= (rest nil) ())
               (= #{0} (realized-by #(do (rest %) nil)))]}
     {:name "next"
      :cases [{:args [(sq "a_0" "a_1" dots)] :out (sq "a_1" dots)}
              {:args [(sq "a_0")] :out nil'}
              {:args [nil'] :out nil'}]
      :checks [(= (next [1 2]) [2]) (nil? (next [1])) (nil? (next nil))
               (= #{0 1} (realized-by #(do (next %) nil)))]}
     {:name "nnext"
      :cases [{:args [(sq "a_0" "a_1" "a_2" dots)] :out (sq "a_2" dots)}
              {:args [(sq "a_0" "a_1")] :out nil'}]
      :checks [(= (nnext [1 2 3]) [3]) (nil? (nnext [1 2]))]}
     {:name "nfirst"
      :cases [{:args [(sq (sq "b_0" "b_1" dots) dots)] :out (sq "b_1" dots)}
              {:args [(sq (sq "b_0") dots)] :out nil'}]
      :checks [(= (nfirst [[1 2 3] [4]]) [2 3]) (nil? (nfirst [[1]]))]}
     {:name "nthrest"
      :cases [{:args [(sq "a_0" dots "a_{n-1}" "a_n" dots) "n"] :out (sq "a_n" dots)}
              {:args [Al "n \\ge l"] :out (sq)}]
      :checks [(= (nthrest [1 2 3] 2) [3]) (= (nthrest [1 2] 5) ())]}
     {:name "nthnext"
      :cases [{:args [(sq "a_0" dots "a_{n-1}" "a_n" dots) "n"] :out (sq "a_n" dots)}
              {:args [Al "n \\ge l"] :out nil'}]
      :checks [(= (nthnext [1 2 3] 2) [3]) (nil? (nthnext [1 2] 5))]}
     {:name "take-nth" :lazy true :xf true
      :cases [{:args ["n" A] :out (sq "a_0" "a_n" "a_{2n}" dots)}]
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
      :cases [{:args [A] :out (str (sq "a_i" dots) (wh "a_i \\ne a_j" (t " for every " " がすべての ") "j < i" (t "" " で成り立つ")))}]
      :checks [(= (distinct [1 2 1 3 2]) [1 2 3]) (lazy-on-infinite? distinct)]}
     {:name "dedupe" :lazy true :xf true
      :cases [{:args [A] :out (str (sq "a_0" "a_i" dots) (wh "a_i \\ne a_{i-1}"))}]
      :checks [(= (dedupe [1 1 2 2 1]) [1 2 1]) (lazy-on-infinite? dedupe)]}
     {:name "random-sample" :lazy true :xf true
      :cases [{:args ["\\mathit{prob}" A] :out (str (sq "a_i" dots) (wh (t "each " "各") "a_i" (t " is kept with probability " " を確率 ") "\\mathit{prob}" (t "" " で残す")))}]
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
      :cases [{:args [F A] :out (str (sq (ap "f" "a_0") (ap "f" "a_1") dots) (wh (ap "f" "a_i") (t " runs in parallel" " を並列に計算する")))}]
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
     {:name "replace" :xf true
      :cases [{:args ["m" A] :lazy true :out (str (sq "r_0" "r_1" dots) (wh "r_i = " (ap "get" "m" "a_i" "a_i")))}
              {:args ["m" (vc "a_0" "a_1" dots)] :lazy false :out (vc "r_0" "r_1" dots)}]
      :checks [(= (replace {1 :one} '(1 2)) [:one 2]) (instance? clojure.lang.LazySeq (replace {1 :one} '(1 2)))
               (= (replace {1 :one} [1 2]) [:one 2]) (vector? (replace {1 :one} [1 2]))]}
     {:name "reductions" :lazy true
      :cases [{:args [F2 A] :out (sq "a_0" (ap "f" "a_0" "a_1") (ap "f" (ap "f" "a_0" "a_1") "a_2") dots)}
              {:args [F2 init A] :out (sq init (ap "f" init "a_0") (ap "f" (ap "f" init "a_0") "a_1") dots)}]
      :checks [(= (reductions + [1 2 3]) [1 3 6]) (= (reductions + 10 [1 2]) [10 11 13])]}]}

   {:id "combining"
    :rows
    [{:name "cons"
      :cases [{:args ["x" A] :out (sq "x" "a_0" "a_1" dots)}]
      :checks [(= (cons 0 [1 2]) [0 1 2]) (= #{} (realized-by #(do (cons :x %) nil)))]}
     {:name "concat" :lazy true
      :cases [{:args [Al Bl dots] :out (sq "a_0" dots "a_{l-1}" "b_0" dots "b_{l'-1}" dots)}]
      :checks [(= (concat [1 2] [3] [4]) [1 2 3 4])]}
     {:name "interleave" :lazy true
      :cases [{:args [(sq "a_0" dots "a_i" "a_{i+1}" (ul "a_{i+2}" dots)) (sq "b_0" dots "b_i")]
               :out (sq "a_0" "b_0" dots "a_i" "b_i")}]
      :checks [(= (interleave [1 2 3] [:a :b]) [1 :a 2 :b])
               (= #{0 1 2} (realized-by #(doall (interleave % [:a :b]))))]}
     {:name "interpose" :lazy true :xf true
      :cases [{:args ["\\mathit{sep}" Al] :out (sq "a_0" "\\mathit{sep}" "a_1" "\\mathit{sep}" dots "\\mathit{sep}" "a_{l-1}")}]
      :checks [(= (interpose 0 [1 2 3]) [1 0 2 0 3])]}
     {:name "cycle" :lazy true
      :cases [{:args [Al] :out (sq "a_0" dots "a_{l-1}" "a_0" dots "a_{l-1}" dots)}]
      :checks [(= (take 5 (cycle [1 2])) [1 2 1 2 1])]}
     {:name "flatten" :lazy true
      :cases [{:args [(sq "a_0" (vc "a_1" (sq "a_2" "a_3")) "a_4")] :out (sq "a_0" "a_1" "a_2" "a_3" "a_4")}]
      :checks [(= (flatten [1 [2 [3 4]] 5]) [1 2 3 4 5])]}]}

   {:id "reordering"
    :rows
    [{:name "reverse"
      :cases [{:args [Al] :out (sq "a_{l-1}" dots "a_0")}]
      :checks [(= (reverse [1 2 3]) [3 2 1])]}
     {:name "sort"
      :cases [{:args [A] :out (str (sq "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh "a_{\\sigma(0)} \\le a_{\\sigma(1)} \\le \\ldots"))}
              {:args ["c" A] :out (str (sq "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh (t "ordered by " "") "c" (t "" " の順に並べる")))}]
      :checks [(= (sort [3 1 2]) [1 2 3]) (= (sort > [3 1 2]) [3 2 1]) (= (sort compare [2 1]) [1 2])]}
     {:name "sort-by"
      :cases [{:args [G A] :out (str (sq "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh (ap kf "a_{\\sigma(0)}") " \\le " (ap kf "a_{\\sigma(1)}") " \\le \\ldots"))}
              {:args [G "c" A] :out (str (sq "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh (ap kf "a_{\\sigma(i)}") (t " ordered by " " を ") "c" (t "" " の順に並べる")))}]
      :checks [(= (sort-by count ["bb" "a" "ccc"]) ["a" "bb" "ccc"]) (= (sort-by first [[1 :b] [0 :x] [1 :a]]) [[0 :x] [1 :b] [1 :a]])
               (= (sort-by count > ["a" "ccc" "bb"]) ["ccc" "bb" "a"])]}
     {:name "shuffle"
      :cases [{:args [A] :out (str (vc "a_{\\sigma(0)}" "a_{\\sigma(1)}" dots) (wh "\\sigma" (t " is a random permutation" " はランダムな置換")))}]
      :checks [(= (sort (shuffle [3 1 2])) [1 2 3]) (vector? (shuffle [1]))]}]}

   {:id "splitting"
    :rows
    [{:name "partition" :lazy true
      :cases [{:args ["n" A] :out (str (sq (sq "a_0" dots "a_{n-1}") (sq "a_n" dots "a_{2n-1}") dots) (wh (t "a short last group is dropped" "最後の組が短ければ捨てる")))}
              {:args ["n" "s" A] :out (sq (sq "a_0" dots "a_{n-1}") (sq "a_s" dots "a_{s+n-1}") dots)}]
      :checks [(= (partition 2 [0 1 2 3 4]) [[0 1] [2 3]]) (= (partition 2 1 [0 1 2]) [[0 1] [1 2]])]}
     {:name "partition-all" :lazy true :xf true
      :cases [{:args ["n" A] :out (str (sq (sq "a_0" dots "a_{n-1}") (sq "a_n" dots "a_{2n-1}") dots) (wh (t "the last group may be short" "最後の組は短いことがある")))}]
      :checks [(= (partition-all 2 [0 1 2 3 4]) [[0 1] [2 3] [4]])]}
     {:name "partition-by" :lazy true :xf true
      :cases [{:args [F (sq "a_0" dots "a_{i-1}" "a_i" dots)]
               :out (str (sq (sq "a_0" dots "a_{i-1}") (sq "a_i" dots) dots) (wh (ap "f" "a_{i-1}") " \\ne " (ap "f" "a_i")))}]
      :checks [(= (partition-by odd? [1 3 2 4 5]) [[1 3] [2 4] [5]])]}
     {:name "split-at"
      :cases [{:args ["n" (sq "a_0" dots "a_{n-1}" "a_n" dots)] :out (vc (sq "a_0" dots "a_{n-1}") (sq "a_n" dots))}]
      :checks [(= (split-at 2 [1 2 3]) [[1 2] [3]]) (vector? (split-at 1 [1]))]}
     {:name "split-with"
      :cases [{:args [P (sq "a_0" dots "a_{i-1}" "a_i" dots)]
               :out (str (vc (sq "a_0" dots "a_{i-1}") (sq "a_i" dots)) (wh (first-wh pa-false)))}]
      :checks [(= (split-with odd? [1 3 2 5]) [[1 3] [2 5]])]}
     {:name "partitionv" :lazy true
      :cases [{:args ["n" A] :out (str (sq (vc "a_0" dots "a_{n-1}") (vc "a_n" dots "a_{2n-1}") dots) (wh (t "a short last group is dropped" "最後の組が短ければ捨てる")))}]
      :checks [(= (partitionv 2 [0 1 2]) [[0 1]]) (vector? (first (partitionv 2 [0 1])))]}
     {:name "partitionv-all" :lazy true :xf true
      :cases [{:args ["n" A] :out (str (sq (vc "a_0" dots "a_{n-1}") (vc "a_n" dots "a_{2n-1}") dots) (wh (t "the last group may be short" "最後の組は短いことがある")))}]
      :checks [(= (partitionv-all 2 [0 1 2]) [[0 1] [2]]) (vector? (first (partitionv-all 2 [0])))]}
     {:name "splitv-at"
      :cases [{:args ["n" (sq "a_0" dots "a_{n-1}" "a_n" dots)] :out (vc (vc "a_0" dots "a_{n-1}") (sq "a_n" dots))}]
      :checks [(= (splitv-at 2 [1 2 3]) [[1 2] [3]]) (vector? (first (splitv-at 1 [1 2])))]}]}

   {:id "reducing"
    :rows
    [{:name "reduce"
      :cases [{:args [F2 A] :out (ap "f" (ap "f" (ap "f" "a_0" "a_1") "a_2") dots)}
              {:args [F2 (sq "a_0")] :out "a_0"}
              {:args [F2 (sq)] :out "(f)"}
              {:args [F2 init A] :out (ap "f" (ap "f" (ap "f" init "a_0") "a_1") dots)}
              {:args [F2 init (sq)] :out init}]
      :checks [(= (reduce - [10 1 2]) 7) (= (reduce + [5]) 5) (= (reduce + []) 0) (= (reduce - 10 [1 2]) 7) (= (reduce + 3 []) 3)]}
     {:name "reduce-kv"
      :cases [{:args ["f : \\mathit{acc}\\ k\\ v \\to \\mathit{acc}" init M]
               :out (ap "f" (ap "f" init "k_0" "v_0") "k_1" "v_1" dots)}]
      :checks [(= (reduce-kv #(+ %1 %2 %3) 0 {1 10 2 20}) 33)]}
     {:name "transduce"
      :cases [{:args [xf (str F2 ",\\ \\mathit{acc} \\to \\mathit{acc}") init A]
               :out (str (ap "f" (ap "f" (ap "f" (ap "f" init "b_0") "b_1") dots)) (wh (sq "b_0" "b_1" dots) " = " (ap "sequence" xf "a")))}]
      :checks [(= (transduce (map inc) + 0 [1 2]) 5)
               (= (transduce (map inc) (fn ([a] (* 100 a)) ([a x] (+ a x))) 0 [1 2]) 500)]}
     {:name "count"
      :cases [{:args [Al] :out "l"} {:args [nil'] :out "0"}]
      :checks [(= (count [1 2 3]) 3) (= (count nil) 0)]}
     {:name "first"
      :cases [{:args [(sq "a_0" (ul "a_1" dots))] :out "a_0"} {:args [(sq)] :out nil'}]
      :checks [(= (first [1 2]) 1) (nil? (first [])) (= #{0} (realized-by first))]}
     {:name "second"
      :cases [{:args [(sq "a_0" "a_1" (ul "a_2" dots))] :out "a_1"} {:args [(sq "a_0")] :out nil'}]
      :checks [(= (second [1 2 3]) 2) (nil? (second [1])) (= #{0 1} (realized-by second))]}
     {:name "fnext"
      :cases [{:args [(sq "a_0" "a_1" (ul "a_2" dots))] :out "a_1"} {:args [(sq "a_0")] :out nil'}]
      :checks [(= (fnext [1 2 3]) 2) (nil? (fnext [1]))]}
     {:name "when-first"
      :cases [{:args [(vc "x" (sq "a_0" (ul "a_1" dots))) "\\mathit{body}"] :out (str "\\mathit{body}" (wh "x = a_0"))}
              {:args [(vc "x" (sq)) "\\mathit{body}"] :out nil'}]
      :checks [(= 7 (when-first [x [7 8]] x)) (nil? (when-first [x []] x)) (= #{0} (realized-by #(when-first [x %] x)))]}
     {:name "last"
      :cases [{:args [Al] :out "a_{l-1}"} {:args [(sq)] :out nil'}]
      :checks [(= (last [1 2 3]) 3) (nil? (last []))]}
     {:name "nth"
      :cases [{:args [(sq "a_0" dots "a_i" (ul dots)) "i"] :out "a_i"}
              {:args [Al "i \\ge l"] :out (t "throws" "例外を投げる")}
              {:args [Al "i \\ge l" "d"] :out "d"}]
      :checks [(= (nth [1 2 3] 1) 2) (= :d (nth [1] 5 :d))
               (= :thrown (try (nth [1] 5) (catch IndexOutOfBoundsException _ :thrown)))
               (= #{0 1 2} (realized-by #(nth % 2)))]}
     {:name "ffirst"
      :cases [{:args [(sq (sq "b_0" (ul dots)) (ul dots))] :out "b_0"}]
      :checks [(= (ffirst [[1 2] [3]]) 1)
               (let [[inner seen] (probe)
                     outer (lazy-seq (cons inner (lazy-seq (swap! seen conj :outer-rest) nil)))]
                 (ffirst outer)
                 (= #{0} @seen))]}
     {:name "rand-nth"
      :cases [{:args [Al] :out (str "a_i" (wh "i" (t " is random" " はランダムに選んだ位置")))}]
      :checks [(contains? #{1 2} (rand-nth [1 2]))]}
     {:name "some"
      :cases [{:args [F (sq "a_0" dots "a_i" (ul dots))]
               :out (cases (ap "f" "a_i") (str (t "wh. " "ただし ") (first-wh (ap "f" "a_i") (t " truthy" " が真")))
                           nil' (t "otherwise" "そのようなiが無いとき"))}]
      :checks [(= (some #(when (> % 1) (* 10 %)) [1 2 3]) 20) (nil? (some even? [1 3]))
               (= #{0 1 2} (realized-by #(some (fn [x] (when (= x 2) x)) %)))]}
     {:name "every?"
      :cases [{:args [P (sq "a_0" dots "a_i" (ul dots))]
               :out (cases false' (str (t "wh. " "ただし ") (first-wh pa-false))
                           true' (t "otherwise" "そのようなiが無いとき"))}]
      :checks [(true? (every? odd? [1 3])) (false? (every? odd? [1 2 3])) (true? (every? odd? []))
               (= #{0 1} (realized-by #(every? (fn [x] (< x 1)) %)))]}
     {:name "not-every?"
      :cases [{:args [P (sq "a_0" dots "a_i" (ul dots))]
               :out (cases true' (str (t "wh. " "ただし ") (first-wh pa-false))
                           false' (t "otherwise" "そのようなiが無いとき"))}]
      :checks [(true? (not-every? odd? [1 2])) (false? (not-every? odd? [1 3]))
               (= #{0 1} (realized-by #(not-every? (fn [x] (< x 1)) %)))]}
     {:name "not-any?"
      :cases [{:args [P (sq "a_0" dots "a_i" (ul dots))]
               :out (cases false' (str (t "wh. " "ただし ") (first-wh pa-true))
                           true' (t "otherwise" "そのようなiが無いとき"))}]
      :checks [(true? (not-any? even? [1 3])) (false? (not-any? even? [1 2]))
               (= #{0 1} (realized-by #(not-any? (fn [x] (= x 1)) %)))]}
     {:name "empty?"
      :cases [{:args [(sq)] :out true'} {:args [(sq "a_0" (ul dots))] :out false'}]
      :checks [(true? (empty? [])) (false? (empty? [1])) (= #{0} (realized-by empty?))]}
     {:name "not-empty"
      :cases [{:args [(sq)] :out nil'} {:args [(sq "a_0" dots)] :out (sq "a_0" dots)}]
      :checks [(nil? (not-empty [])) (= [1] (not-empty [1]))]}
     {:name "distinct?"
      :cases [{:args ["x_0" "x_1" dots] :out (cases true' (str (t "wh. " "") "x_i \\ne x_j" (t " for every " " がすべての ") "i \\ne j" (t "" " で成り立つとき")) false' (t "otherwise" "それ以外のとき"))}]
      :checks [(true? (distinct? 1 2 3)) (false? (distinct? 1 2 1)) (true? (distinct? 1))]}
     {:name "max-key"
      :cases [{:args [(str kf " : x \\to " (t "number")) "x_0" "x_1" dots] :out (str "x_i" (wh (ap kf "x_i") (t " is the largest (the last one on a tie)" " が最大 (同じ値なら最後のもの)")))}]
      :checks [(= "ccc" (max-key count "a" "ccc" "bb")) (= "cd" (max-key count "ab" "cd")) (= "ef" (max-key count "ab" "cd" "ef"))]}
     {:name "min-key"
      :cases [{:args [(str kf " : x \\to " (t "number")) "x_0" "x_1" dots] :out (str "x_i" (wh (ap kf "x_i") (t " is the smallest (the last one on a tie)" " が最小 (同じ値なら最後のもの)")))}]
      :checks [(= "a" (min-key count "a" "ccc" "bb")) (= "cd" (min-key count "ab" "cd")) (= "ef" (min-key count "ab" "cd" "ef"))]}]}

   {:id "collecting"
    :rows
    [{:name "into"
      :cases [{:args [(vc "t_0" dots) A] :out (vc "t_0" dots "a_0" "a_1" dots)}
              {:args [(ls "t_0" dots) A] :out (ls dots "a_1" "a_0" "t_0" dots)}
              {:args [(mp (sp "k_0" "v_0") dots) (sq (vc "k" "v") dots)] :out (mp (sp "k_0" "v_0") dots (sp "k" "v") dots)}
              {:args ["\\mathit{to}" xf A] :out (ap "into" "\\mathit{to}" (ap "sequence" xf "a"))}]
      :checks [(= (into [0] [1 2]) [0 1 2]) (= (into '(0) [1 2]) '(2 1 0)) (= (into {:a 1} [[:b 2]]) {:a 1 :b 2})
               (= (into [] (map inc) [1 2]) [2 3])]}
     {:name "vec"
      :cases [{:args [A] :out (vc "a_0" "a_1" dots)}]
      :checks [(= (vec '(1 2)) [1 2]) (vector? (vec nil))]}
     {:name "set"
      :cases [{:args [A] :out (st "a_0" "a_1" dots)}]
      :checks [(= (set [1 2 1]) #{1 2})]}
     {:name "zipmap"
      :cases [{:args [(sq "k_0" dots "k_i") (sq "v_0" dots "v_i" "v_{i+1}" (ul "v_{i+2}" dots))] :out (mp (sp "k_0" "v_0") dots (sp "k_i" "v_i"))}]
      :checks [(= (zipmap [:a :b] [1 2 3]) {:a 1 :b 2})
               (= #{0 1 2} (realized-by #(zipmap [:a :b] %)))]}
     {:name "frequencies"
      :cases [{:args [A] :out (str (mp (sp "a_i" "\\mathit{cnt}_i") dots) (wh "\\mathit{cnt}_i" (t " is how many times " " は ") "a_i" (t " occurs" " の出現回数")))}]
      :checks [(= (frequencies [:a :b :a]) {:a 2 :b 1})]}
     {:name "group-by"
      :cases [{:args [F A] :out (str (mp (sp "y" (vc "a_i" dots)) dots) (wh (ap "f" "a_i") " = y"))}]
      :checks [(= (group-by odd? [1 2 3]) {true [1 3] false [2]})]}]}

   {:id "collections"
    :rows
    [{:name "conj"
      :cases [{:args [(vc "a_0" dots) "x"] :out (vc "a_0" dots "x")}
              {:args [(ls "a_0" dots) "x"] :out (ls "x" "a_0" dots)}
              {:args [(st "a_0" dots) "x"] :out (st "a_0" dots "x")}
              {:args [(mp (sp "k_0" "v_0") dots) (vc "k" "v")] :out (mp (sp "k_0" "v_0") dots (sp "k" "v"))}
              {:args [nil' "x"] :out (ls "x")}]
      :checks [(= (conj [1] 2) [1 2]) (= (conj '(1) 2) '(2 1)) (= (conj #{1} 2) #{1 2}) (= (conj {:a 1} [:b 2]) {:a 1 :b 2}) (= (conj nil 1) '(1)) (list? (conj nil 1))]}
     {:name "disj"
      :cases [{:args [(st "a_0" dots "x" dots) "x"] :out (st "a_0" dots)}]
      :checks [(= (disj #{1 2} 2) #{1})]}
     {:name "assoc"
      :cases [{:args [M "k" "v"] :out (str (mp (sp "k_0" "v_0") dots (sp "k" "v")) (wh (t "a previous value of " "") "k" (t " is replaced" " の前の値を置き換える")))}
              {:args [(vc "a_0" dots "a_i" dots) "i" "x"] :out (vc "a_0" dots "x" dots)}]
      :checks [(= (assoc {:a 1} :b 2) {:a 1 :b 2}) (= (assoc {:a 1} :a 2) {:a 2}) (= (assoc [1 2] 1 :x) [1 :x]) (= (assoc [1] 1 :x) [1 :x])]}
     {:name "dissoc"
      :cases [{:args [(mp (sp "k_0" "v_0") dots (sp "k" "v")) "k"] :out (mp (sp "k_0" "v_0") dots)}]
      :checks [(= (dissoc {:a 1 :b 2} :b) {:a 1})]}
     {:name "get"
      :cases [{:args [(mp dots (sp "k" "v") dots) "k"] :out "v"}
              {:args ["m" "k"] :out (str nil' (wh "k" (t " is not a key of " " が ") "m" (t "" " のキーでないとき")))}
              {:args ["m" "k" "d"] :out (str "d" (wh "k" (t " is not a key of " " が ") "m" (t "" " のキーでないとき")))}
              {:args [(vc "a_0" dots "a_i" dots) "i"] :out "a_i"}]
      :checks [(= (get {:a 1} :a) 1) (nil? (get {} :a)) (= :d (get {} :a :d)) (= 2 (get [1 2] 1))]}
     {:name "get-in"
      :cases [{:args ["m" (vc "k_0" "k_1" dots)] :out (ap "get" (ap "get" "m" "k_0") "k_1" dots)}]
      :checks [(= (get-in {:a {:b 1}} [:a :b]) 1) (nil? (get-in {:a 1} [:b :c]))]}
     {:name "assoc-in"
      :cases [{:args ["m" (vc "k_0" "k_1") "v"] :out (ap "assoc" "m" "k_0" (ap "assoc" (ap "get" "m" "k_0") "k_1" "v"))}]
      :checks [(= (assoc-in {} [:a :b] 1) {:a {:b 1}})]}
     {:name "update"
      :cases [{:args [(mp dots (sp "k" "v") dots) "k" "f : v \\to w"] :out (mp dots (sp "k" (ap "f" "v")) dots)}
              {:args [(mp dots (sp "k" "v") dots) "k" "f" "x_0" dots] :out (mp dots (sp "k" (ap "f" "v" "x_0" dots)) dots)}]
      :checks [(= (update {:a 1} :a inc) {:a 2}) (= (update {:a 1} :a + 10) {:a 11}) (= (update {} :a (fnil inc 0)) {:a 1})]}
     {:name "update-in"
      :cases [{:args ["m" (vc "k_0" "k_1") "f : v \\to w"] :out (ap "assoc-in" "m" (vc "k_0" "k_1") (ap "f" (ap "get-in" "m" (vc "k_0" "k_1"))))}]
      :checks [(= (update-in {:a {:b 1}} [:a :b] inc) {:a {:b 2}})]}
     {:name "update-keys"
      :cases [{:args [M "f : k \\to k"] :out (mp (sp (ap "f" "k_0") "v_0") (sp (ap "f" "k_1") "v_1") dots)}]
      :checks [(= (update-keys {:a 1} name) {"a" 1})]}
     {:name "update-vals"
      :cases [{:args [M "f : v \\to w"] :out (mp (sp "k_0" (ap "f" "v_0")) (sp "k_1" (ap "f" "v_1")) dots)}]
      :checks [(= (update-vals {:a 1} inc) {:a 2})]}
     {:name "merge"
      :cases [{:args [(mp (sp "k_0" "v_0") dots) (mp (sp "k_0" "w_0") (sp "k_1" "w_1") dots) dots]
               :out (str (mp (sp "k_0" "w_0") (sp "k_1" "w_1") dots) (wh (t "the later map wins" "後のマップの値を取る")))}]
      :checks [(= (merge {:a 1 :b 2} {:a 3 :c 4}) {:a 3 :b 2 :c 4}) (nil? (merge))]}
     {:name "merge-with"
      :cases [{:args ["f : v\\ w \\to v" (mp (sp "k_0" "v_0") dots) (mp (sp "k_0" "w_0") (sp "k_1" "w_1") dots)]
               :out (mp (sp "k_0" (ap "f" "v_0" "w_0")) (sp "k_1" "w_1") dots)}]
      :checks [(= (merge-with + {:a 1 :b 2} {:a 3 :c 4}) {:a 4 :b 2 :c 4})]}
     {:name "select-keys"
      :cases [{:args ["m" (vc "k_0" "k_1" dots)] :out (str (mp (sp "k_i" "v_i") dots) (wh "k_i" (t " is a key of " " は ") "m" (t "" " のキー")))}]
      :checks [(= (select-keys {:a 1 :b 2} [:a :z]) {:a 1})]}
     {:name "contains?"
      :cases [{:args ["m" "k"] :out (cases true' (str (t "wh. " "") "k" (t " is a key of " " が ") "m" (t "" " のキーのとき")) false' (t "otherwise" "それ以外のとき"))}
              {:args [(vc "a_0" dots "a_{l-1}") "i"] :out (cases true' (str (t "wh. " "") "0 \\le i < l" (t "" " のとき")) false' (t "otherwise" "それ以外のとき"))}]
      :checks [(true? (contains? {:a nil} :a)) (false? (contains? {} :a)) (true? (contains? [:x :y] 1)) (false? (contains? [:x :y] :x))]}
     {:name "find"
      :cases [{:args [(mp dots (sp "k" "v") dots) "k"] :out (vc "k" "v")}
              {:args ["m" "k"] :out (str nil' (wh "k" (t " is not a key of " " が ") "m" (t "" " のキーでないとき")))}]
      :checks [(= (find {:a 1} :a) [:a 1]) (map-entry? (find {:a 1} :a)) (nil? (find {} :a))]}
     {:name "key"
      :cases [{:args ["e"] :out (str "k" (wh "e = " (vc "k" "v") (t " is a map entry" " はマップのエントリー")))}]
      :checks [(= :a (key (first {:a 1})))]}
     {:name "val"
      :cases [{:args ["e"] :out (str "v" (wh "e = " (vc "k" "v") (t " is a map entry" " はマップのエントリー")))}]
      :checks [(= 1 (val (first {:a 1})))]}
     {:name "peek"
      :cases [{:args [(vc "a_0" dots "a_{l-1}")] :out "a_{l-1}"}
              {:args [(ls "a_0" dots)] :out "a_0"}
              {:args [(vc)] :out nil'}]
      :checks [(= (peek [1 2 3]) 3) (= (peek '(1 2 3)) 1) (nil? (peek []))]}
     {:name "pop"
      :cases [{:args [(vc "a_0" dots "a_{l-1}")] :out (vc "a_0" dots "a_{l-2}")}
              {:args [(ls "a_0" "a_1" dots)] :out (ls "a_1" dots)}
              {:args [(vc)] :out (t "throws" "例外を投げる")}
              {:args [(ls)] :out (t "throws" "例外を投げる")}]
      :checks [(= (pop [1 2 3]) [1 2]) (= (pop '(1 2 3)) '(2 3))
               (= :thrown (try (pop []) (catch IllegalStateException _ :thrown)))
               (= :thrown (try (pop ()) (catch IllegalStateException _ :thrown)))]}
     {:name "subvec"
      :cases [{:args [(vc "a_0" dots "a_{l-1}") "i" "j"] :out (vc "a_i" dots "a_{j-1}")}
              {:args [(vc "a_0" dots "a_{l-1}") "i"] :out (vc "a_i" dots "a_{l-1}")}]
      :checks [(= (subvec [0 1 2 3] 1 3) [1 2]) (= (subvec [0 1 2] 1) [1 2])]}
     {:name "empty"
      :cases [{:args [(vc "a_0" dots)] :out (vc)}
              {:args [M] :out (mp)}]
      :checks [(= (empty [1 2]) []) (vector? (empty [1])) (= (empty {:a 1}) {}) (map? (empty {:a 1}))
               (sorted? (empty (sorted-map :a 1)))]}]}

   {:id "constructing"
    :rows
    [{:name "list"
      :cases [{:args ["x_0" "x_1" dots] :out (ls "x_0" "x_1" dots)}]
      :checks [(= (list 1 2) '(1 2)) (list? (list 1))]}
     {:name "vector"
      :cases [{:args ["x_0" "x_1" dots] :out (vc "x_0" "x_1" dots)}]
      :checks [(= (vector 1 2) [1 2]) (vector? (vector))]}
     {:name "vector-of"
      :cases [{:args ["\\mathit{type}" "x_0" dots] :out (str (vc "x_0" dots) (wh (t "elements are stored as the primitive " "要素をプリミティブ型の ") "\\mathit{type}" (t "" " で持つ")))}]
      :checks [(= (vector-of :int 1 2) [1 2]) (instance? clojure.core.Vec (vector-of :long))]}
     {:name "hash-map"
      :cases [{:args ["k_0" "v_0" "k_1" "v_1" dots] :out M}]
      :checks [(= (hash-map :a 1 :b 2) {:a 1 :b 2}) (= (hash-map :a 1 :a 2) {:a 2})]}
     {:name "array-map"
      :cases [{:args ["k_0" "v_0" "k_1" "v_1" dots] :out (str M (wh (t "keys stay in the given order" "キーは渡した順のまま")))}]
      :checks [(= (keys (array-map :b 1 :a 2)) [:b :a])]}
     {:name "sorted-map"
      :cases [{:args ["k_0" "v_0" "k_1" "v_1" dots] :out (str (mp (sp "k_{\\sigma(0)}" "v_{\\sigma(0)}") (sp "k_{\\sigma(1)}" "v_{\\sigma(1)}") dots) (wh "k_{\\sigma(0)} < k_{\\sigma(1)} < \\ldots"))}]
      :checks [(= (keys (sorted-map :b 1 :a 2)) [:a :b]) (sorted? (sorted-map))]}
     {:name "sorted-map-by"
      :cases [{:args ["c" "k_0" "v_0" "k_1" "v_1" dots] :out (str (mp (sp "k_{\\sigma(0)}" "v_{\\sigma(0)}") (sp "k_{\\sigma(1)}" "v_{\\sigma(1)}") dots) (wh (t "keys ordered by " "キーを ") "c" (t "" " の順に並べる")))}]
      :checks [(= (keys (sorted-map-by > 1 :a 2 :b)) [2 1])]}
     {:name "hash-set"
      :cases [{:args ["x_0" "x_1" dots] :out (st "x_0" "x_1" dots)}]
      :checks [(= (hash-set 1 2 1) #{1 2})]}
     {:name "sorted-set"
      :cases [{:args ["x_0" "x_1" dots] :out (str (st "x_{\\sigma(0)}" "x_{\\sigma(1)}" dots) (wh "x_{\\sigma(0)} < x_{\\sigma(1)} < \\ldots"))}]
      :checks [(= (seq (sorted-set 3 1 2)) [1 2 3])]}
     {:name "sorted-set-by"
      :cases [{:args ["c" "x_0" "x_1" dots] :out (str (st "x_{\\sigma(0)}" "x_{\\sigma(1)}" dots) (wh (t "ordered by " "") "c" (t "" " の順に並べる")))}]
      :checks [(= (seq (sorted-set-by > 1 3 2)) [3 2 1])]}]}

   {:id "predicates"
    :rows
    [(pred-row "coll?" "a list, vector, map, set, map entry or seq" "リスト、ベクター、マップ、セット、マップのエントリー、seqのどれか"
               [(= (true-for coll?) #{:list :vector :seq :lazy :map :set :entry :sorted-map :sorted-set})])
     (pred-row "seq?" "a list or a seq" "リストかseq"
               [(= (true-for seq?) #{:list :seq :lazy})])
     (pred-row "seqable?" "a collection, nil, a string or a Java array" "コレクション、nil、文字列、Javaの配列のどれか"
               [(= (true-for seqable?) (set (keys samples)))])
     (pred-row "sequential?" "a list, vector, map entry or seq" "リスト、ベクター、マップのエントリー、seqのどれか"
               [(= (true-for sequential?) #{:list :vector :seq :lazy :entry})])
     (pred-row "associative?" "a map, vector or map entry" "マップ、ベクター、マップのエントリーのどれか"
               [(= (true-for associative?) #{:map :sorted-map :vector :entry})])
     (pred-row "counted?" "a list, vector, map, set or map entry, or a seq that knows its length, such as a vector's seq" "リスト、ベクター、マップ、セット、マップのエントリーのどれかか、ベクターのseqのように長さを知っているseq"
               [(= (true-for counted?) #{:list :vector :seq :map :set :entry :sorted-map :sorted-set})
                (false? (counted? (cons 1 '(2)))) (true? (counted? (range 10)))])
     (pred-row "reversible?" "a vector, map entry, sorted map or sorted set" "ベクター、マップのエントリー、ソート済みマップ、ソート済みセットのどれか"
               [(= (true-for reversible?) #{:vector :entry :sorted-map :sorted-set})])
     (pred-row "sorted?" "a sorted map or sorted set" "ソート済みマップかソート済みセット"
               [(= (true-for sorted?) #{:sorted-map :sorted-set})])
     (pred-row "indexed?" "a vector or map entry" "ベクターかマップのエントリー"
               [(= (true-for indexed?) #{:vector :entry})])
     (pred-row "list?" "a list" "リスト"
               [(= (true-for list?) #{:list})])
     (pred-row "vector?" "a vector or map entry" "ベクターかマップのエントリー"
               [(= (true-for vector?) #{:vector :entry})])
     (pred-row "map?" "a map" "マップ"
               [(= (true-for map?) #{:map :sorted-map})])
     (pred-row "set?" "a set" "セット"
               [(= (true-for set?) #{:set :sorted-set})])
     (pred-row "map-entry?" "a map entry" "マップのエントリー"
               [(= (true-for map-entry?) #{:entry})])]}

   {:id "transients"
    :rows
    [{:name "transient"
      :cases [{:args [(vc "a_0" dots)] :out (tr "a_0" dots)}
              {:args [M] :out (tm (sp "k_0" "v_0") (sp "k_1" "v_1") dots)}
              {:args [(st "a_0" dots)] :out (ts "a_0" dots)}]
      :checks [(= [1] (persistent! (transient [1]))) (= {:a 1} (persistent! (transient {:a 1}))) (= #{1} (persistent! (transient #{1})))]}
     {:name "persistent!"
      :cases [{:args [(tr "a_0" dots)] :out (str (vc "a_0" dots) (wh (t "the transient can no longer be used" "transientはもう使えない")))}]
      :checks [(let [t (transient [1])] (persistent! t) (= :thrown (try (conj! t 2) (catch IllegalAccessError _ :thrown))))]}
     {:name "conj!"
      :cases [{:args [(tr "a_0" dots) "x"] :out (tr "a_0" dots "x")}]
      :checks [(= [1 2] (persistent! (conj! (transient [1]) 2)))]}
     {:name "assoc!"
      :cases [{:args [(tm (sp "k_0" "v_0") dots) "k" "v"] :out (tm (sp "k_0" "v_0") dots (sp "k" "v"))}
              {:args [(tr "a_0" dots "a_i" dots) "i" "x"] :out (tr "a_0" dots "x" dots)}]
      :checks [(= {:a 1 :b 2} (persistent! (assoc! (transient {:a 1}) :b 2))) (= [:x 2] (persistent! (assoc! (transient [1 2]) 0 :x)))]}
     {:name "dissoc!"
      :cases [{:args [(tm (sp "k_0" "v_0") dots (sp "k" "v")) "k"] :out (tm (sp "k_0" "v_0") dots)}]
      :checks [(= {:a 1} (persistent! (dissoc! (transient {:a 1 :b 2}) :b)))]}
     {:name "disj!"
      :cases [{:args [(ts "a_0" dots "x" dots) "x"] :out (ts "a_0" dots)}]
      :checks [(= #{1} (persistent! (disj! (transient #{1 2}) 2)))]}
     {:name "pop!"
      :cases [{:args [(tr "a_0" dots "a_{l-1}")] :out (tr "a_0" dots "a_{l-2}")}]
      :checks [(= [1 2] (persistent! (pop! (transient [1 2 3]))))]}]}

   {:id "values"
    :rows
    [{:name "="
      :cases [{:args ["x_0" "x_1" dots] :out (cases true' (str (t "wh. every " "すべての ") "x_i" (t " is equal to " " が ") "x_0" (t "" " と等しいとき")) false' (t "otherwise" "それ以外のとき"))}]
      :checks [(true? (= [1 2] '(1 2))) (false? (= [1] #{1})) (true? (= 1 1 1)) (false? (= 1 1.0))]}
     {:name "not="
      :cases [{:args ["x_0" "x_1" dots] :out (ap "not" (ap "=" "x_0" "x_1" dots))}]
      :checks [(true? (not= 1 2)) (false? (not= [1] '(1)))]}
     {:name "identical?"
      :cases [{:args ["x" "y"] :out (cases true' (str (t "wh. " "") "x" (t " and " " と ") "y" (t " are the same object" " が同じオブジェクトのとき")) false' (t "otherwise" "それ以外のとき"))}]
      :checks [(let [v [1]] (true? (identical? v v))) (false? (identical? [1] [1]))]}
     {:name "compare"
      :cases [{:args ["x" "y"] :out (cases (t "a negative number" "負の数") (str (t "wh. " "") "x" (t " comes before " " が ") "y" (t "" " より前のとき")) "0" (str (t "wh. " "") "x" (t " and " " と ") "y" (t " are in the same place" " が同じ位置のとき")) (t "a positive number" "正の数") (str (t "wh. " "") "x" (t " comes after " " が ") "y" (t "" " より後のとき")))}]
      :checks [(neg? (compare 1 2)) (zero? (compare [1 2] [1 2])) (pos? (compare "b" "a")) (neg? (compare nil 1)) (zero? (compare 1 1.0)) (pos? (compare [1 2] [3]))]}
     {:name "nil?"
      :cases [{:args [nil'] :out true'} {:args ["x"] :out (str false' (wh "x \\ne " nil'))}]
      :checks [(true? (nil? nil)) (false? (nil? false))]}
     {:name "some?"
      :cases [{:args [nil'] :out false'} {:args ["x"] :out (str true' (wh "x \\ne " nil'))}]
      :checks [(false? (some? nil)) (true? (some? false))]}
     {:name "true?"
      :cases [{:args [true'] :out true'} {:args ["x"] :out (str false' (wh "x \\ne " true'))}]
      :checks [(true? (true? true)) (false? (true? 1))]}
     {:name "false?"
      :cases [{:args [false'] :out true'} {:args ["x"] :out (str false' (wh "x \\ne " false'))}]
      :checks [(true? (false? false)) (false? (false? nil))]}
     {:name "not"
      :cases [{:args [(str nil' (t " or " " か ") false')] :out true'} {:args ["x"] :out (str false' (wh (t "any other " "") "x" (t "" " がそれ以外の値のとき")))}]
      :checks [(true? (not nil)) (true? (not false)) (false? (not 0))]}
     {:name "instance?"
      :cases [{:args ["\\mathit{class}" "x"] :out (cases true' (str (t "wh. " "") "x" (t " is an instance of " " が ") "\\mathit{class}" (t "" " のインスタンスのとき")) false' (t "otherwise" "それ以外のとき"))}]
      :checks [(true? (instance? Long 1)) (false? (instance? String 1))]}
     {:name "record?"
      :cases [{:args ["x"] :out (cases true' (str (t "wh. " "") "x" (t " is a record made by " " が ") "\\mathtt{defrecord}" (t "" " で作ったレコードのとき")) false' (t "otherwise" "それ以外のとき"))}]
      :checks [(false? (record? {:a 1}))]}]}

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
      :cases [{:args ["f" "d"] :out (lam (sq "x" dots) (str (ap "f" "x'" dots) (wh "x' = d" (t " if " " (") "x = " nil' (t ", else " " のとき)、") "x' = x" (t "" " (それ以外のとき)"))))}]
      :checks [(= ((fnil inc 0) nil) 1) (= ((fnil inc 0) 5) 6) (= ((fnil not 0) false) true)]}
     {:name "every-pred"
      :cases [{:args ["p" "q" dots] :out (lam "x" (ap "boolean" (ap "and" (ap "p" "x") (ap "q" "x") dots)))}]
      :checks [(true? ((every-pred odd? pos?) 1)) (false? ((every-pred odd? pos?) -1))]}
     {:name "some-fn"
      :cases [{:args ["f" "g" dots] :out (lam "x" (ap "or" (ap "f" "x") (ap "g" "x") dots))}]
      :checks [(= ((some-fn :a :b) {:b 2}) 2) (nil? ((some-fn :a :b) {}))]}
     {:name "trampoline"
      :cases [{:args ["f" "x_0" dots] :out (str "r" (wh "r" (t " is the first value for which " " は ") "\\mathtt{fn?}" (t " is false in " " が偽になる最初の値で、調べる値の並びは ") (sq (ap "f" "x_0" dots) (ap (ap "f" "x_0" dots)) dots)))}]
      :checks [(= 5 (trampoline (fn [] (fn [] 5)))) (letfn [(g [x] (if (< x 3) #(g (inc x)) x))] (= 3 (trampoline g 0)))]}
     {:name "memoize"
      :cases [{:args [F] :out (str "f'" (wh (ap "f'" "x") (t " calls " " は ") (ap "f" "x") (t " once per distinct " " を異なる ") "x" (t "" " ごとに1回呼ぶ")))}]
      :checks [(let [n (atom 0) g (memoize (fn [x] (swap! n inc) x))] (g 1) (g 1) (g 2) (= 2 @n))]}]}])

(defn math [s]
  (str "<span class=\"math\" data-tex=\""
       (.encodeToString (Base64/getEncoder) (.getBytes ^String s "UTF-8"))
       "\"></span>"))

(defn doc-url [name]
  (str "https://clojure.github.io/clojure/clojure.core-api.html#clojure.core/" name))

(def placeholder #"\u0001([^\u0002]*)\u0002([^\u0003]*)\u0003")

(defn localize [lang s]
  (let [s (str/replace s placeholder
                       (fn [[_ en ja]]
                         (let [x (if (= lang :ja) ja en)]
                           (if (= x "") "" (str "\\text{" x "}")))))]
    (if (= lang :ja)
      (-> (loop [s s]
            (let [s' (str/replace s #"\\text\{([^}]*)\}\\text\{([^}]*)\}" "\\\\text{$1$2}")]
              (if (= s s') s (recur s'))))
          (str/replace #"(?<=[\p{IsHan}\p{IsHiragana}\p{IsKatakana}]) (?=[\p{IsHan}\p{IsHiragana}\p{IsKatakana}])" ""))
      s)))

(defn strip-text [s] (str/replace s #"\\text\{[^}]*\}" ""))

(defn escape [s] (str/escape s {\< "&lt;" \> "&gt;" \& "&amp;" \" "&quot;"}))

(defn row-html [lang {:keys [name lazy xf cases]}]
  (let [n (count cases)
        span (fn [body] (str "<td" (when (> n 1) (str " rowspan=\"" n "\"")) ">" body "</td>"))
        per-case-lazy? (some #(contains? % :lazy) cases)
        mark (fn [b] (if b "✓" ""))]
    (str/join
     "\n"
     (map-indexed
      (fn [i {:keys [args out] :as c}]
        (str "<tr>"
             (when (zero? i)
               (span (str "<a href=\"" (doc-url name) "\"><code style=\"white-space: nowrap\">" (escape name) "</code></a>")))
             "<td>" (if (seq args) (str/join ", " (map (comp math (partial localize lang)) args)) "") "</td>"
             "<td>" (math (localize lang out)) "</td>"
             (if per-case-lazy?
               (str "<td>" (mark (:lazy c)) "</td>")
               (when (zero? i) (span (mark lazy))))
             (when (zero? i) (span (mark xf)))
             "</tr>"))
      cases))))

(def headers
  {:en ["Function" "Arguments" "Result" "Lazy" "xf"]
   :ja ["関数" "引数" "結果" "遅延" "xf"]})

(defn table-html [lang {:keys [rows]}]
  (str "<table>\n<thead><tr>" (apply str (map #(str "<th>" % "</th>") (headers lang))) "</tr></thead>\n<tbody>\n"
       (str/join "\n" (map (partial row-html lang) rows))
       "\n</tbody>\n</table>\n"))

(defmacro checked-sections []
  `[~@(for [{:keys [id rows]} sections]
        `{:id ~id
          :results [~@(for [{:keys [name checks]} rows]
                        `[~name [~@(for [c checks] `[(quote ~c) (boolean ~c)])]])]})])

(defn transducer-arity?
  "True when the arglists have [args...] next to [args... coll] or
  [args... & colls], the shape of a function whose collection-less arity
  returns a transducer. conj and conj! have that shape for their
  reducing-function arities."
  [name]
  (let [arglists (set (map vec (:arglists (meta (ns-resolve 'clojure.core (symbol name))))))
        coll-arg? (fn [sym] (and (symbol? sym) (re-matches #"coll|c\d+" (clojure.core/name sym))))]
    (and (not (#{"conj" "conj!"} name))
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
                      name)
        texs (for [{:keys [name cases]} rows
                   {:keys [args out]} cases
                   tex (cons out args)]
               [name tex])
        untranslated (for [[name tex] texs
                           [_ en ja] (re-seq placeholder tex)
                           :when (or (and (= en "") (= ja ""))
                                     (and (not= en "") (= en ja) (not (#{"bool" "number"} en))))]
                       [name en])
        math-mismatch (for [[name tex] texs
                            :when (not= (strip-text (localize :en tex)) (strip-text (localize :ja tex)))]
                        name)]
    (doseq [[name form] failures] (println "FAIL" name (pr-str form)))
    (doseq [name missing] (println "NOT IN clojure.core" name))
    (doseq [name dupes] (println "DUPLICATE" name))
    (doseq [name xf-mismatch] (println "XF MISMATCH" name))
    (doseq [[name en] untranslated] (println "UNTRANSLATED" name (pr-str en)))
    (doseq [name math-mismatch] (println "MATH MISMATCH" name))
    (when (or (seq failures) (seq missing) (seq dupes) (seq xf-mismatch) (seq untranslated) (seq math-mismatch)) (System/exit 1))
    (doseq [[lang dir] [[:en (io/file out-dir)] [:ja (io/file out-dir "ja")]]]
      (io/make-parents (io/file dir "x"))
      (doseq [{:keys [id] :as section} sections]
        (spit (io/file dir (str id ".html")) (table-html lang section))))
    (println "ok" (clojure-version)
             (count rows) "functions"
             (count (mapcat :checks rows)) "checks")))

(apply -main *command-line-args*)
