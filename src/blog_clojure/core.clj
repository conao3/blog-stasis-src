(ns blog-clojure.core
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [blog-clojure.portfolio :as portfolio]
    [clojure.string :as str]
    [hiccup.page :as hiccup.page]
    [hiccup2.core :as hiccup]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.content-type :as content-type]
    [stasis.core :as stasis]))


(def blog-name "Conao3 Notes")
(def site-url "https://s.conao3.com")
(def site-description
  "conao3 (Naoya Yamashita) の技術ブログ。Emacs Lisp、Lean 4、Clojure、Rust、Python、TypeScript で作ったものと調べたことを書いています。")
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


(defn link-images
  [node]
  (loop [child (.getFirstChild node)]
    (when child
      (let [next-child (.getNext child)]
        (when (or (instance? org.commonmark.node.Image child)
                  (instance? org.commonmark.node.Link child))
          (.setDestination child (str/replace (.getDestination child) #"^(\.\./)+resources/public/" "/")))
        (if (and (instance? org.commonmark.node.Image child)
                 (not (instance? org.commonmark.node.Link node)))
          (let [link (org.commonmark.node.Link. (.getDestination child) nil)]
            (.insertBefore child link)
            (.appendChild link child))
          (link-images child))
        (recur next-child))))
  node)


(defn inline-text
  [node]
  (cond
    (nil? node) ""
    (instance? org.commonmark.node.Text node) (.getLiteral node)
    (instance? org.commonmark.node.Code node) (.getLiteral node)
    :else (->> (take-while some? (iterate #(.getNext %) (.getFirstChild node)))
               (map inline-text)
               (apply str))))


(defn join-cjk-lines
  [node]
  (let [wide? #(re-matches #"[\p{IsHan}\p{IsHiragana}\p{IsKatakana}\p{InCJKSymbolsAndPunctuation}\p{InHalfwidthAndFullwidthForms}]" (str %))
        alnum? #(re-matches #"[\p{IsLatin}\p{Nd}]" (str %))]
    (loop [child (.getFirstChild node)]
      (when child
        (let [next-child (.getNext child)]
          (if (instance? org.commonmark.node.SoftLineBreak child)
            (let [before (last (inline-text (.getPrevious child)))
                  after (first (inline-text next-child))]
              (when (or (and (wide? before) (or (wide? after) (alnum? after)))
                        (and (alnum? before) (wide? after)))
                (.unlink child)))
            (join-cjk-lines child))
          (recur next-child)))))
  node)


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
                      str/trim)]
    (cond
      (str/blank? text) nil
      (< 120 (count text)) (str (subs text 0 119) "…")
      :else text)))


(defn demote-headings
  [document]
  (doseq [node (tree-seq some?
                         #(take-while some? (iterate (fn [node] (.getNext node)) (.getFirstChild %)))
                         document)
          :when (instance? org.commonmark.node.Heading node)]
    (.setLevel node (min 6 (inc (.getLevel node)))))
  document)


(defn parse-markdown
  [s]
  (let [[header body] (str/split s #"\n\n" 2)
        metadata (->> (str/split-lines header)
                      (keep #(re-matches #"(\w+):\s*(.*)" %))
                      (map (fn [[_ k v]] [(keyword (str/lower-case k)) v]))
                      (into {}))
        document (join-cjk-lines (.parse markdown-parser (or body "")))]
    {:metadata metadata
     :description (markdown-description document)
     :html (.render markdown-renderer (link-images (demote-headings document)))}))


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


(defn rfc3339
  [datetime]
  (let [s (str/replace datetime " " "T")
        local (java.time.LocalDateTime/parse
                (cond
                  (re-matches #"\d{4}-\d{2}-\d{2}" s) (str s "T00:00")
                  :else (subs s 0 (min 19 (count s)))))
        zone (if (neg? (compare s "2026-04-01")) "Asia/Tokyo" "America/Toronto")]
    (.format (.atZone local (java.time.ZoneId/of zone))
             java.time.format.DateTimeFormatter/ISO_OFFSET_DATE_TIME)))


(defn markdown-path
  [path]
  (str/replace path #"\.html$" ".md"))


(def author
  {"@type" "Person"
   "name" "Naoya Yamashita"
   "alternateName" "conao3"
   "url" (str site-url (portfolio/urls :en))
   "image" (str site-url "/assets/icon.jpg")
   "jobTitle" "Software Engineer"
   "sameAs" ["https://github.com/conao3"
             "https://x.com/conao_3"
             "https://www.linkedin.com/in/conao3/"
             portfolio/tech-book-fest]})


(defn decode-tex
  [s]
  (str/trim (String. (.decode (java.util.Base64/getDecoder) s) "UTF-8")))


(defn plain-markdown
  [body]
  (-> body
      (str/replace #"<div class=\"github-card\" data-repo=\"([^\"]+)\"><a href=\"([^\"]+)\">.*?(?:<span class=\"github-card-description\">(.*?)</span>.*?)?</a></div>"
                   (fn [[_ repo href description]]
                     (str "[" repo "](" href ")" (when description (str ": " description)))))
      (str/replace #"<(?:span|div) class=\"math display\" data-tex=\"([^\"]*)\"></(?:span|div)>"
                   (fn [[_ tex]] (str "$$" (decode-tex tex) "$$")))
      (str/replace #"<span class=\"math\" data-tex=\"([^\"]*)\"></span>"
                   (fn [[_ tex]] (str "$" (decode-tex tex) "$")))
      (str/replace #"<span class=\"footnote\">(?:<sup>, </sup>)?<sup><button [^>]*>(\d+)</button></sup><span class=\"footnote-body\"[^>]*>.*?</span></span>"
                   "[^$1]")
      (str/replace #"(?m)^<sup><a id=\"fn\.(\d+)\" href=\"#fnr\.\d+\">\d+</a></sup> " "[^$1]: ")
      (str/replace #"\]\((?:\.\./)+resources/public/" (str "](" site-url "/"))
      (str/replace #"\n{3,}" "\n\n")
      str/trim))


(defn post-markdown
  [{:keys [title date url]} body]
  (str "# " title "\n\n"
       (when date (str "Date: " date "\n"))
       "URL: " url "\n"
       "Author: conao3 (Naoya Yamashita)\n\n"
       (plain-markdown body) "\n"))


(defn post-list-markdown
  [posts]
  (->> posts
       (map (fn [[path {:keys [title date]}]]
              (str "- " date " [" title "](" site-url (markdown-path path) ")")))
       (str/join "\n")))


(defn index-markdown
  [posts]
  (str "# " blog-name "\n\n"
       "conao3 (Naoya Yamashita) の技術ブログです。Emacs Lisp、Lean 4、Clojure、Rust、Python、TypeScript などで作ったものと調べたことを日本語で書いています。\n\n"
       "- Portfolio (English): " site-url (portfolio/urls :en) "\n"
       "- ポートフォリオ: " site-url (portfolio/urls :ja) "\n"
       "- GitHub: https://github.com/conao3\n"
       "- X: https://x.com/conao_3\n"
       "- Sitemap: " site-url "/sitemap.xml\n\n"
       "各記事は、記事の URL に `Accept: text/markdown` を付けて取るか、末尾に `.md` を付けた URL で Markdown として読めます。\n\n"
       "## Blogs\n\n"
       (post-list-markdown posts) "\n"))


(defn llms-txt
  [posts]
  (str "# " blog-name "\n\n"
       "> conao3 (Naoya Yamashita) の個人の技術ブログ。Emacs Lisp のパッケージ (leaf.el など)、Lean 4 で証明した関数を他の言語から呼ぶライブラリ (lean-leanfran)、Clojure・Rust・Python・TypeScript での開発の記録を日本語で書いている。\n\n"
       "## When to use\n\n"
       "- conao3 が作った OSS (leaf.el、ox-zenn、lean-clj、lean-rs、lean-py、awsr、plamo-tr-local など) の使い方・設計・作った理由を調べるとき。\n"
       "- Emacs の設定と Emacs Lisp、Lean 4 での証明と他の言語からの呼び出し、Rust・Python・Clojure のツール作りについて、日本語の実例を探すとき。\n"
       "- conao3 の経歴や活動を確かめるとき (Portfolio のページ)。\n\n"
       "## How to read\n\n"
       "- 記事の URL に `Accept: text/markdown` を付けて GET すると Markdown が返る。末尾に `.md` を付けた URL でも同じ Markdown を取れる。\n"
       "- 全ページの一覧は " site-url "/sitemap.xml にある。\n\n"
       "## Pages\n\n"
       "- [Portfolio (English)](" site-url (portfolio/urls :en) ")\n"
       "- [ポートフォリオ](" site-url (portfolio/urls :ja) ")\n\n"
       "## Blogs\n\n"
       (post-list-markdown posts) "\n"))


(def not-found-markdown
  (str "# 404 Not Found\n\n"
       "お探しのページは、移動したか削除された可能性があります。\n"
       "The page you are looking for may have been moved or deleted.\n\n"
       "- Home: " site-url "/\n"
       "- Sitemap: " site-url "/sitemap.xml\n"
       "- llms.txt: " site-url "/llms.txt\n"))


(defn xml-escape
  [s]
  (-> (str s)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))


(defn atom-feed
  [posts]
  (let [entries (take 30 posts)]
    (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
         "<feed xmlns=\"http://www.w3.org/2005/Atom\" xml:lang=\"ja\">\n"
         "  <title>" (xml-escape blog-name) "</title>\n"
         "  <subtitle>" (xml-escape site-description) "</subtitle>\n"
         "  <id>" site-url "/</id>\n"
         "  <link rel=\"alternate\" type=\"text/html\" href=\"" site-url "/\"/>\n"
         "  <link rel=\"self\" type=\"application/atom+xml\" href=\"" site-url "/feed.xml\"/>\n"
         "  <updated>" (rfc3339 (:datetime (second (first entries)))) "</updated>\n"
         "  <author><name>conao3 (Naoya Yamashita)</name><uri>" site-url (portfolio/urls :en) "</uri></author>\n"
         "  <icon>" site-url "/assets/icon.jpg</icon>\n"
         (->> entries
              (map (fn [[path {:keys [title url datetime description lang]}]]
                     (str "  <entry>\n"
                          "    <title>" (xml-escape title) "</title>\n"
                          "    <id>" url "</id>\n"
                          "    <link rel=\"alternate\" type=\"text/html\" href=\"" url "\"/>\n"
                          "    <published>" (rfc3339 datetime) "</published>\n"
                          "    <updated>" (rfc3339 datetime) "</updated>\n"
                          (when description (str "    <summary xml:lang=\"" (or lang "ja") "\">" (xml-escape description) "</summary>\n"))
                          "  </entry>\n")))
              (apply str))
         "</feed>\n")))


(defn sitemap-xml
  [pages]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
       "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
       (->> pages
            (sort-by first)
            (map (fn [[path {:keys [date]}]]
                   (str "  <url><loc>" (page-url path) "</loc>"
                        (when date (str "<lastmod>" date "</lastmod>"))
                        "</url>\n")))
            (apply str))
       "</urlset>\n"))


(def ai-user-agents
  ["GPTBot" "OAI-SearchBot" "ChatGPT-User" "ClaudeBot" "Claude-User" "Claude-SearchBot"
   "PerplexityBot" "Perplexity-User" "Google-Extended" "Applebot-Extended" "CCBot"])


(def robots-txt
  (str "User-agent: *\n"
       "Content-Signal: search=yes, ai-input=yes, ai-train=no\n"
       "Allow: /\n\n"
       (->> ai-user-agents
            (map #(str "User-agent: " % "\n"))
            (apply str))
       "Content-Signal: search=yes, ai-input=yes, ai-train=no\n"
       "Allow: /\n\n"
       "Sitemap: " site-url "/sitemap.xml\n"))


(defn json-ld
  [path {:keys [title description lang datetime post? og-type]}]
  (cond
    (= path "/index.html")
    {"@context" "https://schema.org"
     "@graph" [{"@type" "WebSite"
                "name" blog-name
                "url" (str site-url "/")
                "inLanguage" "ja"
                "description" site-description
                "author" author}
               (assoc author "description" (get-in portfolio/pages [:en :description]))]}

    post?
    {"@context" "https://schema.org"
     "@graph" [{"@type" "BlogPosting"
                "headline" title
                "description" description
                "url" (page-url path)
                "mainEntityOfPage" (page-url path)
                "datePublished" (rfc3339 datetime)
                "inLanguage" (or lang "ja")
                "image" (str site-url (og-image-path path))
                "isPartOf" {"@type" "Blog" "name" blog-name "url" (str site-url "/")}
                "author" author
                "publisher" author}
               {"@type" "BreadcrumbList"
                "itemListElement" [{"@type" "ListItem" "position" 1 "name" blog-name "item" (str site-url "/")}
                                   {"@type" "ListItem" "position" 2 "name" title "item" (page-url path)}]}]}

    (= og-type "profile")
    {"@context" "https://schema.org"
     "@type" "ProfilePage"
     "url" (page-url path)
     "inLanguage" lang
     "mainEntity" (assoc author "description" description)}))


(def og-locales
  {"ja" "ja_JP" "en" "en_US"})


(defn render-page
  [path {:keys [title body description lang og-type draft? portfolio-href not-found? markdown? post? datetime alternates] :as page}]
  (hiccup.page/html5 {:lang (or lang "ja")}
                     [:head
                      [:meta {:http-equiv "Content-Type" :content "text/html; charset=utf-8"}]
                      [:meta {:name "viewport" :content "width=device-width,initial-scale=1"}]
                      [:title (if (= title blog-name) blog-name (format "%s - %s" title blog-name))]
                      [:meta {:name "author" :content "conao3 (Naoya Yamashita)"}]
                      [:meta {:name "theme-color" :content "#1d1d1d"}]
                      [:link {:rel "icon" :href "/assets/icon.jpg"}]
                      [:link {:rel "apple-touch-icon" :href "/assets/icon.jpg"}]
                      [:link {:rel "alternate" :type "application/atom+xml" :title blog-name :href "/feed.xml"}]
                      (for [[hreflang href] alternates]
                        [:link {:rel "alternate" :hreflang hreflang :href (str site-url href)}])
                      (when-not not-found?
                        [:link {:rel "canonical" :href (page-url path)}])
                      (when markdown?
                        [:link {:rel "alternate" :type "text/markdown" :href (markdown-path path)}])
                      (when not-found?
                        [:meta {:name "robots" :content "noindex"}])
                      (when description
                        [:meta {:name "description" :content description}])
                      [:meta {:property "og:title" :content title}]
                      [:meta {:property "og:type" :content (or og-type (if (= path "/index.html") "website" "article"))}]
                      [:meta {:property "og:url" :content (page-url path)}]
                      (when-not not-found?
                        (list [:meta {:property "og:image" :content (str site-url (og-image-path path))}]
                              [:meta {:property "og:image:width" :content "1200"}]
                              [:meta {:property "og:image:height" :content "630"}]
                              [:meta {:property "og:image:alt" :content title}]))
                      [:meta {:property "og:site_name" :content blog-name}]
                      [:meta {:property "og:locale" :content (get og-locales (or lang "ja") "ja_JP")}]
                      (when (and post? datetime)
                        (list [:meta {:property "article:published_time" :content (rfc3339 datetime)}]
                              [:meta {:property "article:author" :content (str site-url (portfolio/urls :en))}]))
                      (when description
                        [:meta {:property "og:description" :content description}])
                      [:meta {:name "twitter:card" :content "summary_large_image"}]
                      [:meta {:name "twitter:site" :content "@conao_3"}]
                      [:meta {:name "twitter:creator" :content "@conao_3"}]
                      (when-let [data (json-ld path page)]
                        [:script {:type "application/ld+json"}
                         (hiccup/raw (str/replace (json/write-str data :escape-slash false :escape-unicode false) "</" "<\\/"))])
                      [:style (hiccup/raw (inline-style))]
                      [:script {:type "speculationrules"} (hiccup/raw speculation-rules)]
                      (when (str/includes? body "data-tex")
                        [:script {:src "/assets/math.js" :defer true}])
                      (when (str/includes? body "class=\"github-card\"")
                        [:script {:src "/assets/github-card.js" :defer true}])]
                     [:body
                      [:header
                       [:a {:class "site-title" :href "/"}
                        [:img {:src "/assets/icon.jpg" :alt "" :width 32 :height 32}]
                        blog-name]
                       [:nav
                        (when portfolio-href
                          [:a {:href portfolio-href} (if (= lang "en") "Portfolio" "ポートフォリオ")])]]
                      (when draft?
                        [:p {:class "draft-banner"} "下書き: 開発用サーバーでだけ表示し、本番のビルドには出しません。"])
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
  [{:keys [title body date datetime url post?]}]
  (hiccup/html
    [:article
     [:header
      [:h1 title]
      (when post?
        [:div {:class "post-meta"}
         [:time {:datetime datetime} date]
         [:a {:class "share-x" :href (x-share-url title url) :target "_blank" :rel "noopener"}
          (hiccup/raw x-logo)
          "ポスト"]])]
     [:div {:class "content"} (hiccup/raw body)]]))


(defn site
  ([] (site {:drafts? true}))
  ([{:keys [drafts?]}]
  (let [portfolio-langs (->> (keys portfolio/paths)
                             (filter #(or drafts? (not (get-in portfolio/pages [% :draft?]))))
                             set)
        contents (->> (stasis/slurp-directory "generated/contents" #"\.md$")
                      (map (fn [[k v]]
                             (let [p (parse-markdown v)
                                   path (str (subs k 0 (- (count k) 3)) ".html")
                                   obj {:title (:title (:metadata p))
                                        :url (page-url path)
                                        :post? (not= path "/index.html")
                                        :date (some-> (:date (:metadata p)) (subs 0 10))
                                        :datetime (some-> (:date (:metadata p)) (str/replace " " "T"))
                                        :lang (:lang (:metadata p))
                                        :draft? (= "true" (some-> (:draft (:metadata p)) str/lower-case))
                                        :description (:description p)
                                        :markdown? true
                                        :source (second (str/split v #"\n\n" 2))
                                        :body (:html p)}]
                               [path (assoc obj :body (render-content obj))])))
                      (into {})
                      (merge (into {}
                                   (map (fn [[lang path]]
                                          [path {:title (get-in portfolio/pages [lang :title])
                                                 :description (get-in portfolio/pages [lang :description])
                                                 :lang (name lang)
                                                 :og-type "profile"
                                                 :draft? (get-in portfolio/pages [lang :draft?])
                                                 :portfolio? true
                                                 :body (portfolio/body lang portfolio-langs)}]))
                                   (select-keys portfolio/paths portfolio-langs)))
                      (remove (fn [[_ page]] (and (:draft? page) (not drafts?))))
                      (into {}))
        portfolio-href (fn [lang]
                         (let [lang (if (= lang "en") :en :ja)]
                           (when (contains? contents (portfolio/paths lang))
                             (portfolio/urls lang))))
        blog-inx (hiccup/html
                   [:section {:class "post-list"}
                    [:h2 "Blogs"]
                    [:ul
                     (->> (dissoc contents "/index.html")
                          (remove (comp :portfolio? second))
                          (sort-by (fn [[k v]] [(:datetime v) k]))
                          reverse
                          (map (fn [[k v]] [:li [:time {:datetime (:datetime v)} (:date v)] [:a {:href k} (:title v)] (when (:draft? v) [:span {:class "draft-label"} "下書き"])])))]])
        contents (-> contents
                     (update-in ["/index.html" :body] #(str % blog-inx))
                     (assoc "/404.html" {:title "ページが見つかりません"
                                         :not-found? true
                                         :body (hiccup/html
                                                 [:article
                                                  [:header [:h1 "ページが見つかりません"]]
                                                  [:div {:class "content"}
                                                   [:p "お探しのページは、移動したか削除された可能性があります。"]
                                                   [:p {:lang "en"} "The page you are looking for may have been moved or deleted."]
                                                   [:p [:a {:href "/"} "トップページへ"] " / " [:a {:href (portfolio/urls :ja)} "ポートフォリオ"] " / " [:a {:href (portfolio/urls :en) :lang "en"} "Portfolio"]]]])}))
        posts (->> contents
                   (filter (comp :post? second))
                   (sort-by (fn [[k v]] [(:datetime v) k]))
                   reverse)
        portfolio-alternates (let [langs (filter #(contains? contents (portfolio/paths %)) [:en :ja])]
                               (when (< 1 (count langs))
                                 (concat (map (fn [lang] [(name lang) (portfolio/urls lang)]) langs)
                                         [["x-default" (portfolio/urls :en)]])))
        contents (-> contents
                     (update "/index.html" assoc
                             :description site-description
                             :date (:date (second (first posts))))
                     (update-vals #(cond-> % (:portfolio? %) (assoc :alternates portfolio-alternates))))]
    (stasis/merge-page-sources
      {:contents (into {} (map (fn [[path page]] [path (render-page path (assoc page :portfolio-href (portfolio-href (:lang page))))])) contents)
       :markdown (-> (into {}
                           (map (fn [[path page]] [(markdown-path path) (post-markdown page (:source page))]))
                           posts)
                     (assoc "/index.md" (index-markdown posts)
                            "/404.md" not-found-markdown))
       :agents {"/sitemap.xml" (sitemap-xml (dissoc contents "/404.html"))
                "/robots.txt" robots-txt
                "/llms.txt" (llms-txt posts)
                "/feed.xml" (atom-feed posts)}
       :og-images (into {}
                        (keep (fn [[path page]]
                               (when-not (:not-found? page)
                               [(og-image-path path)
                                (fn [_] (og-image (if (= path "/index.html") {:title blog-name} page)))])))
                        contents)
       :public (slurp-binary-directory "resources/public" #"\.[^.]+$")
       :spectrum (-> (stasis/slurp-directory "generated/spectrum" #"\.[^.]+$")
                     (update-keys (partial str "/assets/spectrum")))}))))


(defn export
  [& _args]
  (let [export-dir "./target"
        load-export-dir #(stasis/slurp-directory export-dir #"\.[^.]+$")
        old-files (load-export-dir)]
    (stasis/empty-directory! export-dir)
    (println "Exporting...")
    (stasis/export-pages (site {:drafts? false}) "target/" stasis-config)
    (io/copy (io/file "resources/_redirects") (io/file export-dir "_redirects"))
    (println "Export complete:")
    (stasis/report-differences old-files (load-export-dir))))


(defn- wrap-extensionless
  [handler]
  (fn [req]
    (let [uri (:uri req)
          html-req (when (and (not= uri "/") (not (re-find #"\.[^/]*$" uri)))
                     (assoc req :uri (str (str/replace uri #"/$" "") ".html")))
          res (when html-req (handler html-req))]
      (if (and res (= 200 (:status res)))
        res
        (handler req)))))

(defn start-server
  [& _args]
  (jetty/run-jetty
    (content-type/wrap-content-type (wrap-extensionless (stasis/serve-pages site stasis-config)))
    {:port 8080 :join? false}))
