(ns user
  (:require
    [babashka.fs :as fs]
    [blog-clojure.core :as blog-clojure]
    [clojure.data.json :as json]
    [clojure.java.io :as io]))


(defonce server (atom nil))


(defn go
  []
  (when @server
    (.stop @server))
  (->> (blog-clojure/start-server)
       (reset! server)))


(defn generate-css-1
  [file]
  (letfn [(getfn
            [m]
            (let [schema (:$schema m)
                  known-value-types #{"https://opensource.adobe.com/spectrum-tokens/schemas/token-types/alias.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/color.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/opacity.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/dimension.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/multiplier.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/font-weight.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/font-size.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/text-transform.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/font-family.json"
                                      "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/font-style.json"}]
              (cond
                (= "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/color-set.json"
                   schema)
                (getfn (get-in m [:sets :darkest]))

                (= "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/system-set.json"
                   schema)
                (getfn (get-in m [:sets :spectrum]))

                (= "https://opensource.adobe.com/spectrum-tokens/schemas/token-types/scale-set.json"
                   schema)
                (getfn (get-in m [:sets :desktop]))

                (known-value-types schema)
                (:value m)

                :else
                (throw (ex-info "unknown schema" {:schema schema})))))
          (format-val
            [v]
            (if-let [m (and (string? v)
                            (re-matches #"\{(.*)\}" v))]
              (format "var(--%s)" (second m))
              (cond->> v (double? v) (format "%.2f"))))]
    (->
      (json/read-str (slurp (io/resource (format "spectrum/%s.json" file))) :key-fn keyword)
      (update-vals getfn)
      (->> (sort-by (fn [[k _]]
                      (if-let [m (re-matches #"^(.*)-([0-9]+)$" (name k))]
                        (str (nth m 1) (format "%05d" (parse-long (nth m 2))))
                        (name k))))
           (map (fn [[k v]] (format "--%s: %s;\n" (name k) (format-val v))))
           (apply str)
           (format ":root {\n%s}\n")
           (spit (format "generated/spectrum/%s.css" file))))))


(defn generate-css
  []
  (doseq [file ["color-aliases"
                "color-component"
                "color-palette"
                "icons"
                "layout"
                "layout-component"
                "semantic-color-palette"
                "typography"]]
    (fs/create-dirs "generated/spectrum")
    (println (format "generating %s.css..." file))
    (generate-css-1 file)))
