(ns blog-clojure.core
  (:require
   [clojure.java.io :as io]
   [markdown.core :as markdown]
   [hiccup.page :as hiccup.page]
   [hiccup2.core :as hiccup]
   [stasis.core :as stasis]
   [ring.adapter.jetty :as jetty]))

(def blog-name "Conao3 Notes")
(def stasis-config {:stasis/ignore-nil-pages? true})

(defn slurp-binary-directory [dir re]
  (->> (file-seq (io/file dir))
       (filter (fn [file] (and (.isFile file) (re-find re (.getName file)))))
       (map (fn [file]
              (let [relative-path (subs (.getPath file) (count dir))
                    path (str "/" (clojure.string/replace relative-path #"^[\\/]+" ""))]
                [path (with-open [in (io/input-stream file)]
                        (let [out (java.io.ByteArrayOutputStream.)]
                          (io/copy in out)
                          (.toByteArray out)))])))
       (into {})))

(defn render-page [{:keys [title body]}]
  (hiccup.page/html5 {:lang "ja"}
   [:head
    [:meta {:http-equiv "Content-Type" :content "text/html; charset=utf-8"}]
    [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
    [:title (format "%s - %s" title blog-name)]
    (hiccup.page/include-css "/assets/modern-css-reset.css")
    (hiccup.page/include-css "/assets/spectrum/color-palette.css")
    (hiccup.page/include-css "/assets/spectrum/typography.css")
    (hiccup.page/include-css "/assets/index.css")]
   [:body
    [:h2 blog-name]
    [:div {:style {:display "flex"}}
     [:a {:href "/"} "Home"]]
    (hiccup/raw body)]))

(defn render-content [{:keys [title body date]}]
  (hiccup/html
   [:article
    [:h1 title]
    [:div date]
    [:hr]
    (hiccup/raw body)]))

(defn site []
  (let [contents (->> (stasis/slurp-directory "generated/contents" #"\.md$")
                      (map (fn [[k v]]
                             (let [p (markdown/md-to-html-string-with-meta v :heading-anchors true)
                                   obj {:title (first (:title (:metadata p)))
                                        :date (first (:date (:metadata p)))
                                        :body (:html p)}]
                               [(str (subs k 0 (- (count k) 3)) ".html")
                                (assoc obj :body (render-content obj))])))
                      (into {}))
        blog-inx (hiccup/html
                  [:div
                   [:h1 "Blogs"]
                   [:ul
                    (->> contents
                         (map (fn [[k v]] [:li [:a {:href k} (:title v)]]))
                         (sort-by (comp :date second))
                         reverse)]])
        contents (update-in contents ["/index.html" :body]
                            #(str % blog-inx))]
    (stasis/merge-page-sources
     {:contents (-> contents
                    (update-vals render-page)
                    (->> (into {})))
      :public (slurp-binary-directory "resources/public" #"\.[^.]+$")
      :spectrum (-> (stasis/slurp-directory "generated/spectrum" #"\.[^.]+$")
                    (update-keys (partial str "/assets/spectrum")))})))

(defn export [& _args]
  (let [export-dir "./target"
        load-export-dir #(stasis/slurp-directory export-dir #"\.[^.]+$")
        old-files (load-export-dir)]
    (stasis/empty-directory! export-dir)
    (println "Exporting...")
    (stasis/export-pages (site) "target/" stasis-config)
    (println "Export complete:")
    (stasis/report-differences old-files (load-export-dir))))

(defn start-server [& _args]
  (jetty/run-jetty
    (stasis/serve-pages site stasis-config)
    {:port 8080 :join? false}))
