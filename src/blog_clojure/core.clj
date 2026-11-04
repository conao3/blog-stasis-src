(ns blog-clojure.core
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [hiccup.page :as hiccup.page]
    [hiccup2.core :as hiccup]
    [ring.adapter.jetty :as jetty]
    [stasis.core :as stasis]))


(def blog-name "Conao3 Notes")
(def stasis-config {:stasis/ignore-nil-pages? true})


(def markdown-extensions
  [(org.commonmark.ext.gfm.tables.TablesExtension/create)
   (org.commonmark.ext.gfm.strikethrough.StrikethroughExtension/create)
   (org.commonmark.ext.heading.anchor.HeadingAnchorExtension/create)])


(def markdown-parser
  (-> (org.commonmark.parser.Parser/builder) (.extensions markdown-extensions) .build))


(def lazy-image-attributes
  (reify org.commonmark.renderer.html.AttributeProviderFactory
    (create [_ _]
      (reify org.commonmark.renderer.html.AttributeProvider
        (setAttributes [_ node _ attributes]
          (when (instance? org.commonmark.node.Image node)
            (.put attributes "loading" "lazy")))))))


(def markdown-renderer
  (-> (org.commonmark.renderer.html.HtmlRenderer/builder)
      (.extensions markdown-extensions)
      (.attributeProviderFactory lazy-image-attributes)
      .build))


(def stylesheets
  ["resources/public/assets/modern-css-reset.css"
   "generated/spectrum/color-palette.css"
   "generated/spectrum/typography.css"
   "generated/spectrum/layout.css"
   "resources/public/assets/index.css"])


(defn prune-custom-properties
  [css]
  (let [declaration #"(?m)^--([\w-]+):(.*);$"
        definitions (into {} (map (fn [[_ k v]] [k v]) (re-seq declaration css)))
        references #(map second (re-seq #"var\(--([\w-]+)" %))
        used (loop [used #{}
                    pending (references (str/replace css declaration ""))]
               (if-let [[k & more] (seq pending)]
                 (if (used k)
                   (recur used more)
                   (recur (conj used k) (concat more (references (get definitions k "")))))
                 used))]
    (str/replace css declaration #(if (used (nth % 1)) (first %) ""))))


(defn inline-style
  []
  (->> (map slurp stylesheets)
       (str/join "\n")
       prune-custom-properties
       (#(str/replace % #"\n{2,}" "\n"))))


(def speculation-rules
  "{\"prerender\":[{\"where\":{\"href_matches\":\"/*\"},\"eagerness\":\"moderate\"}]}")


(defn parse-markdown
  [s]
  (let [[header body] (str/split s #"\n\n" 2)
        metadata (->> (str/split-lines header)
                      (keep #(re-matches #"(\w+):\s*(.*)" %))
                      (map (fn [[_ k v]] [(keyword (str/lower-case k)) v]))
                      (into {}))]
    {:metadata metadata
     :html (.render markdown-renderer (.parse markdown-parser (or body "")))}))


(defn slurp-binary-directory
  [dir re]
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


(defn render-page
  [{:keys [title body]}]
  (hiccup.page/html5 {:lang "ja"}
                     [:head
                      [:meta {:http-equiv "Content-Type" :content "text/html; charset=utf-8"}]
                      [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
                      [:title (format "%s - %s" title blog-name)]
                      [:style (hiccup/raw (inline-style))]
                      [:script {:type "speculationrules"} (hiccup/raw speculation-rules)]
                      (when (str/includes? body "data-tex")
                        [:script {:src "/assets/math.js" :defer true}])]
                     [:body
                      [:header
                       [:a {:class "site-title" :href "/"} blog-name]]
                      [:main (hiccup/raw body)]
                      [:footer
                       [:a {:href "/"} blog-name]
                       [:span "© conao3"]]]))


(defn render-content
  [{:keys [title body date]}]
  (hiccup/html
    [:article
     [:header
      [:h1 title]
      [:time date]]
     [:div {:class "content"} (hiccup/raw body)]]))


(defn site
  []
  (let [contents (->> (stasis/slurp-directory "generated/contents" #"\.md$")
                      (map (fn [[k v]]
                             (let [p (parse-markdown v)
                                   obj {:title (:title (:metadata p))
                                        :date (:date (:metadata p))
                                        :body (:html p)}]
                               [(str (subs k 0 (- (count k) 3)) ".html")
                                (assoc obj :body (render-content obj))])))
                      (into {}))
        blog-inx (hiccup/html
                   [:section {:class "post-list"}
                    [:h2 "Blogs"]
                    [:ul
                     (->> (dissoc contents "/index.html")
                          (sort-by (comp :date second))
                          reverse
                          (map (fn [[k v]] [:li [:time (:date v)] [:a {:href k} (:title v)]])))]])
        contents (update-in contents ["/index.html" :body]
                            #(str % blog-inx))]
    (stasis/merge-page-sources
      {:contents (-> contents
                     (update-vals render-page)
                     (->> (into {})))
       :public (slurp-binary-directory "resources/public" #"\.[^.]+$")
       :spectrum (-> (stasis/slurp-directory "generated/spectrum" #"\.[^.]+$")
                     (update-keys (partial str "/assets/spectrum")))})))


(defn export
  [& _args]
  (let [export-dir "./target"
        load-export-dir #(stasis/slurp-directory export-dir #"\.[^.]+$")
        old-files (load-export-dir)]
    (stasis/empty-directory! export-dir)
    (println "Exporting...")
    (stasis/export-pages (site) "target/" stasis-config)
    (println "Export complete:")
    (stasis/report-differences old-files (load-export-dir))))


(defn start-server
  [& _args]
  (jetty/run-jetty
    (stasis/serve-pages site stasis-config)
    {:port 8080 :join? false}))
