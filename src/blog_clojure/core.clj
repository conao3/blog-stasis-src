(ns blog-clojure.core
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [hiccup.page :as hiccup.page]
    [hiccup2.core :as hiccup]
    [ring.adapter.jetty :as jetty]
    [stasis.core :as stasis]))


(def blog-name "Conao3 Notes")
(def site-url "https://s.conao3.com")
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


(defn node-worker
  [script]
  (delay
    (let [process (-> (ProcessBuilder. ["node" script])
                      (.redirectError java.lang.ProcessBuilder$Redirect/INHERIT)
                      .start)]
      {:writer (io/writer (.getOutputStream process))
       :reader (io/reader (.getInputStream process))})))


(defn node-request
  [worker request]
  (let [{:keys [writer reader]} @worker]
    (locking worker
      (.write writer (str (json/write-str request) "\n"))
      (.flush writer)
      (json/read-str (.readLine reader) :key-fn keyword))))


(def highlighter (node-worker "bin/highlight.mjs"))
(def og-image-renderer (node-worker "bin/og-image.mjs"))


(defn highlight
  [lang code]
  (:html (node-request highlighter {:lang lang :code (str/replace code #"\n$" "")})))


(defn og-image
  [{:keys [title date]}]
  (.decode (java.util.Base64/getDecoder)
           (:png (node-request og-image-renderer {:title title :date date :site blog-name :host (.getHost (java.net.URI. site-url))}))))


(def highlighted-code-blocks
  (reify org.commonmark.renderer.html.HtmlNodeRendererFactory
    (create [_ context]
      (reify org.commonmark.renderer.NodeRenderer
        (getNodeTypes [_] #{org.commonmark.node.FencedCodeBlock})
        (render [_ node]
          (let [writer (.getWriter context)]
            (.line writer)
            (.raw writer (highlight (first (str/split (or (.getInfo node) "") #"\s+")) (.getLiteral node)))
            (.line writer)))))))


(def markdown-renderer
  (-> (org.commonmark.renderer.html.HtmlRenderer/builder)
      (.extensions markdown-extensions)
      (.attributeProviderFactory lazy-image-attributes)
      (.nodeRendererFactory highlighted-code-blocks)
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


(defn markdown-description
  [document]
  (let [paragraph (->> (tree-seq some?
                                 #(take-while some? (iterate (fn [node] (.getNext node)) (.getFirstChild %)))
                                 document)
                       (filter #(instance? org.commonmark.node.Paragraph %))
                       first)
        text (some->> paragraph
                      (tree-seq some? #(take-while some? (iterate (fn [node] (.getNext node)) (.getFirstChild %))))
                      (keep #(cond
                               (instance? org.commonmark.node.Text %) (.getLiteral %)
                               (instance? org.commonmark.node.Code %) (.getLiteral %)
                               (instance? org.commonmark.node.SoftLineBreak %) " "))
                      (apply str)
                      (#(str/replace % #"(?<=[\p{IsHan}\p{IsHiragana}\p{IsKatakana}、。）」])\s+(?=[\p{IsHan}\p{IsHiragana}\p{IsKatakana}（「])" ""))
                      str/trim)]
    (cond
      (str/blank? text) nil
      (< 120 (count text)) (str (subs text 0 119) "…")
      :else text)))


(defn parse-markdown
  [s]
  (let [[header body] (str/split s #"\n\n" 2)
        metadata (->> (str/split-lines header)
                      (keep #(re-matches #"(\w+):\s*(.*)" %))
                      (map (fn [[_ k v]] [(keyword (str/lower-case k)) v]))
                      (into {}))
        document (.parse markdown-parser (or body ""))]
    {:metadata metadata
     :description (markdown-description document)
     :html (.render markdown-renderer document)}))


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


(defn page-url
  [path]
  (str site-url (str/replace path #"(index)?\.html$" "")))


(defn og-image-path
  [path]
  (str "/og" (str/replace path #"\.html$" ".png")))


(defn render-page
  [path {:keys [title body description]}]
  (hiccup.page/html5 {:lang "ja"}
                     [:head
                      [:meta {:http-equiv "Content-Type" :content "text/html; charset=utf-8"}]
                      [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
                      [:title (format "%s - %s" title blog-name)]
                      [:link {:rel "icon" :href "/assets/icon.jpg"}]
                      (when description
                        [:meta {:name "description" :content description}])
                      [:meta {:property "og:title" :content title}]
                      [:meta {:property "og:type" :content (if (= path "/index.html") "website" "article")}]
                      [:meta {:property "og:url" :content (page-url path)}]
                      [:meta {:property "og:image" :content (str site-url (og-image-path path))}]
                      [:meta {:property "og:site_name" :content blog-name}]
                      (when description
                        [:meta {:property "og:description" :content description}])
                      [:meta {:name "twitter:card" :content "summary_large_image"}]
                      [:style (hiccup/raw (inline-style))]
                      [:script {:type "speculationrules"} (hiccup/raw speculation-rules)]
                      (when (str/includes? body "data-tex")
                        [:script {:src "/assets/math.js" :defer true}])]
                     [:body
                      [:header
                       [:a {:class "site-title" :href "/"}
                        [:img {:src "/assets/icon.jpg" :alt "" :width 32 :height 32}]
                        blog-name]]
                      [:main (hiccup/raw body)]
                      [:footer
                       [:a {:href "/"} blog-name]
                       [:span "© conao3"]]]))


(def x-logo
  "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z\"/></svg>")


(defn x-share-url
  [title url]
  (let [encode #(str/replace (java.net.URLEncoder/encode % "UTF-8") "+" "%20")]
    (str "https://x.com/intent/post?text=" (encode title) "&url=" (encode url))))


(defn render-content
  [{:keys [title body date url]}]
  (hiccup/html
    [:article
     [:header
      [:h1 title]
      [:div {:class "post-meta"}
       [:time date]
       [:a {:class "share-x" :href (x-share-url title url) :target "_blank" :rel "noopener" :title "X でシェア" :aria-label "X でシェア"}
        (hiccup/raw x-logo)]]]
     [:div {:class "content"} (hiccup/raw body)]]))


(defn site
  []
  (let [contents (->> (stasis/slurp-directory "generated/contents" #"\.md$")
                      (map (fn [[k v]]
                             (let [p (parse-markdown v)
                                   path (str (subs k 0 (- (count k) 3)) ".html")
                                   obj {:title (:title (:metadata p))
                                        :url (page-url path)
                                        :date (:date (:metadata p))
                                        :description (:description p)
                                        :body (:html p)}]
                               [path (assoc obj :body (render-content obj))])))
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
      {:contents (into {} (map (fn [[path page]] [path (render-page path page)])) contents)
       :og-images (into {}
                        (map (fn [[path page]]
                               [(og-image-path path)
                                (fn [_] (og-image (if (= path "/index.html") {:title blog-name} page)))]))
                        contents)
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
