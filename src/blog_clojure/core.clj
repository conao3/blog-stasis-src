(ns blog-clojure.core
  (:require
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [blog-clojure.portfolio :as portfolio]
    [blog-clojure.sites :as sites]
    [clojure.string :as str]
    [hiccup.page :as hiccup.page]
    [hiccup2.core :as hiccup]
    [ring.adapter.jetty :as jetty]
    [ring.middleware.content-type :as content-type]
    [ring.middleware.file :as file]
    [ring.middleware.not-modified :as not-modified]
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


(defn org-path
  [path]
  (str/replace path #"\.html$" ".org"))


(defn preview-path
  [source-path]
  (str "/preview" source-path ".html"))


(def repo-url "https://github.com/conao3/blog-stasis-src")


(defn git-revision
  []
  (let [{:keys [exit out]} (shell/sh "git" "rev-parse" "HEAD")]
    (if (zero? exit) (str/trim out) "master")))


(defn org-subtrees
  [text]
  (let [lines (vec (str/split text #"\n" -1))
        starts (keep-indexed (fn [i line] (when (str/starts-with? line "* ") i)) lines)]
    (->> (map vector starts (concat (rest starts) [(count lines)]))
         (keep (fn [[start end]]
                 (let [end (loop [end end]
                             (if (and (> end (inc start)) (str/blank? (lines (dec end)))) (recur (dec end)) end))
                       drawer (take-while #(not (re-matches #"(?i):end:\s*" %)) (subvec lines (inc start) end))
                       file-name (some #(second (re-matches #"(?i):export_file_name:\s*generated/contents(/\S+?)(?:\.md)?\s*" %)) drawer)]
                   (when file-name
                     {:path (str file-name ".html")
                      :text (str (str/join "\n" (subvec lines start end)) "\n")
                      :lines [(inc start) end]}))))
         (group-by :path)
         (#(update-vals % (fn [[subtree & more]]
                            (when more
                              (throw (ex-info (str "Duplicate export_file_name: " (:path subtree)) {:path (:path subtree)})))
                            subtree))))))


(defn source-github-url
  [revision [start end]]
  (str repo-url "/blob/" revision "/contents.org?plain=1#L" start "-L" end))


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
      (str/replace #"<div class=\"tweet-card\"><a href=\"([^\"]+)\">.*?<span class=\"tweet-card-author\"><strong>[^<]*</strong><span>([^<]+)</span></span>.*?((?:<span class=\"tweet-card-text\">.*?</span>)*)(?:<span class=\"tweet-card-media\".*?)?<time .*?</a></div>"
                   (fn [[_ href user texts]]
                     (let [paragraphs (map second (re-seq #"<span class=\"tweet-card-text\">(.*?)</span>" texts))]
                       (str "[" user "](" href ")" (when (seq paragraphs) (str ": " (str/join "\n\n" paragraphs)))))))
      (str/replace #"<div class=\"youtube-card\" data-id=\"[^\"]+\"><a href=\"([^\"]+)\">.*?(?:<span class=\"youtube-card-title\">(.*?)</span>.*?)?</a></div>"
                   (fn [[_ href title]]
                     (str "[" (or title "YouTube") "](" href ")")))
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


(defn tag-path
  [tag]
  (str "/tags/" tag ".html"))


(defn parse-tags
  [s]
  (let [tags (some-> s str/trim not-empty (str/split #"\s+"))]
    (doseq [tag tags]
      (when-not (re-matches #"[a-z0-9-]+" tag)
        (throw (ex-info (str "Invalid tag: " tag) {:tag tag}))))
    (vec tags)))


(defn post-markdown
  [{:keys [title date updated url tags]} body]
  (str "# " title "\n\n"
       (when date (str "Date: " date "\n"))
       (when updated (str "Updated: " updated "\n"))
       (when (seq tags) (str "Tags: " (str/join ", " tags) "\n"))
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
       "- サイト: " site-url sites/url "\n"
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
       "- [ポートフォリオ](" site-url (portfolio/urls :ja) ")\n"
       "- [サイト](" site-url sites/url ")\n\n"
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
         "  <updated>" (->> entries (map (fn [[_ {:keys [datetime updated-datetime]}]] (rfc3339 (or updated-datetime datetime)))) sort last) "</updated>\n"
         "  <author><name>conao3 (Naoya Yamashita)</name><uri>" site-url (portfolio/urls :en) "</uri></author>\n"
         "  <icon>" site-url "/assets/icon.jpg</icon>\n"
         (->> entries
              (map (fn [[path {:keys [title url datetime updated-datetime description lang]}]]
                     (str "  <entry>\n"
                          "    <title>" (xml-escape title) "</title>\n"
                          "    <id>" url "</id>\n"
                          "    <link rel=\"alternate\" type=\"text/html\" href=\"" url "\"/>\n"
                          "    <published>" (rfc3339 datetime) "</published>\n"
                          "    <updated>" (rfc3339 (or updated-datetime datetime)) "</updated>\n"
                          (when description (str "    <summary xml:lang=\"" (or lang "ja") "\">" (xml-escape description) "</summary>\n"))
                          "  </entry>\n")))
              (apply str))
         "</feed>\n")))


(defn feed-page
  [feed-xml date]
  (let [feed-url (str site-url "/feed.xml")
        lines (str/split-lines feed-xml)
        rest-count (dec (count (filter #(= "  <entry>" %) lines)))
        head (let [n (count (take-while #(not= "  </entry>" %) lines))]
               (str/join "\n" (concat (take (inc n) lines) [(str "  <!-- 残りの " rest-count " 件の entry -->") "</feed>"])))]
    {:title "フィード"
     :description "Conao3 Notes の新着記事を配る Atom フィードの URL と中身。"
     :date date
     :body (hiccup/html
             [:article
              [:header [:h1 "フィード"]]
              [:div {:class "content"}
               [:p "新着記事を Atom フィード (XML) で配っています。Feedly や Inoreader などのフィードリーダーに、次の URL を登録してください。"]
               [:div {:class "feed-copy"}
                [:input {:type "text" :readonly true :value feed-url :aria-label "フィードの URL"}]
                [:button {:type "button" :hidden true} "コピー"]]
               [:p [:a {:href "/feed.xml"} "feed.xml を開く"]]
               [:h2 "feed.xml の冒頭"]
               [:p "新しい順に記事のタイトル・URL・日時・要約が入っています。最初の 1 件までを抜き出すと次のとおりです。"]
               (hiccup/raw (highlight "xml" head))]])}))


(defn sitemap-xml
  [pages]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
       "<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n"
       (->> pages
            (sort-by first)
            (map (fn [[path {:keys [date updated]}]]
                   (str "  <url><loc>" (page-url path) "</loc>"
                        (when-let [lastmod (or updated date)] (str "<lastmod>" lastmod "</lastmod>"))
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
  [path {:keys [title description lang datetime updated-datetime post? og-type tags]}]
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
     "@graph" [(cond-> {"@type" "BlogPosting"
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
                 updated-datetime (assoc "dateModified" (rfc3339 updated-datetime))
                 (seq tags) (assoc "keywords" tags))
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


(def feed-icon
  "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><circle cx=\"5\" cy=\"19\" r=\"2.5\"/><path d=\"M2.5 9.5a12 12 0 0 1 12 12h-3a9 9 0 0 0-9-9z\"/><path d=\"M2.5 2.5a19 19 0 0 1 19 19h-3a16 16 0 0 0-16-16z\"/></svg>")


(def external-link
  (re-pattern (str "<a (?![^>]*\\btarget=)([^>]*\\bhref=\"https?://(?!"
                   (java.util.regex.Pattern/quote (str/replace site-url #"^https?://" ""))
                   "[/\"])[^\"]*\"[^>]*)>")))


(defn open-external-links-in-new-tab
  [html]
  (str/replace html external-link "<a $1 target=\"_blank\" rel=\"noopener noreferrer\">"))


(defn render-page
  [path {:keys [title body description lang og-type draft? portfolio-href not-found? preview? markdown? post? datetime updated-datetime alternates] :as page}]
  (open-external-links-in-new-tab
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
                      (when-not (or not-found? preview?)
                        [:link {:rel "canonical" :href (page-url path)}])
                      (when markdown?
                        [:link {:rel "alternate" :type "text/markdown" :href (markdown-path path)}])
                      (when (or not-found? preview?)
                        [:meta {:name "robots" :content "noindex"}])
                      (when description
                        [:meta {:name "description" :content description}])
                      [:meta {:property "og:title" :content title}]
                      [:meta {:property "og:type" :content (or og-type (if (= path "/index.html") "website" "article"))}]
                      [:meta {:property "og:url" :content (page-url path)}]
                      (when-not (or not-found? preview?)
                        (list [:meta {:property "og:image" :content (str site-url (og-image-path path))}]
                              [:meta {:property "og:image:width" :content "1200"}]
                              [:meta {:property "og:image:height" :content "630"}]
                              [:meta {:property "og:image:alt" :content title}]))
                      [:meta {:property "og:site_name" :content blog-name}]
                      [:meta {:property "og:locale" :content (get og-locales (or lang "ja") "ja_JP")}]
                      (when (and post? datetime)
                        (list [:meta {:property "article:published_time" :content (rfc3339 datetime)}]
                              (when updated-datetime
                                [:meta {:property "article:modified_time" :content (rfc3339 updated-datetime)}])
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
                      (when (str/includes? body "class=\"feed-copy\"")
                        [:script {:src "/assets/feed.js" :defer true}])
                      (when (str/includes? body "class=\"source-copy\"")
                        [:script {:src "/assets/source.js" :defer true}])
                      (when (str/includes? body "class=\"github-card\"")
                        [:script {:src "/assets/github-card.js" :defer true}])
                      (when (str/includes? body "class=\"youtube-card\"")
                        [:script {:src "/assets/youtube-card.js" :defer true}])
                      (when (str/includes? body "data-oss-map=\"true\"")
                        [:script {:src "/assets/oss-map.js" :defer true}])]
                     [:body
                      [:header
                       [:a {:class "site-title" :href "/"}
                        [:img {:src "/assets/icon.jpg" :alt "" :width 32 :height 32}]
                        blog-name]
                       [:nav
                        [:a {:href "/tags/"} "Tags"]
                        [:a {:class "feed-link" :href "/feed/" :aria-label "Feed"}
                         (hiccup/raw feed-icon)
                         [:span {:class "feed-label"} "Feed"]]
                        [:a {:href sites/url} "Sites"]
                        (when portfolio-href
                          [:a {:href portfolio-href} "Portfolio"])]]
                      (when draft?
                        [:p {:class "draft-banner"} "下書き: 開発用サーバーでだけ表示し、本番のビルドには出しません。"])
                      [:main (hiccup/raw body)]
                      [:footer
                       [:a {:href "/"} blog-name]
                       [:span "© conao3"]]])))


(def calendar-icon
  "<svg viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M4.75 0a.75.75 0 0 1 .75.75V2h5V.75a.75.75 0 0 1 1.5 0V2h1.25c.966 0 1.75.784 1.75 1.75v10.5A1.75 1.75 0 0 1 13.25 16H2.75A1.75 1.75 0 0 1 1 14.25V3.75C1 2.784 1.784 2 2.75 2H4V.75A.75.75 0 0 1 4.75 0ZM2.5 7.5v6.75c0 .138.112.25.25.25h10.5a.25.25 0 0 0 .25-.25V7.5Zm10.75-4H2.75a.25.25 0 0 0-.25.25V6h11V3.75a.25.25 0 0 0-.25-.25Z\"/></svg>")

(def pencil-icon
  "<svg viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M11.013 1.427a1.75 1.75 0 0 1 2.474 0l1.086 1.086a1.75 1.75 0 0 1 0 2.474l-8.61 8.61c-.21.21-.47.364-.756.445l-3.251.93a.75.75 0 0 1-.927-.928l.929-3.25c.081-.286.235-.547.445-.758l8.61-8.61Zm.176 4.823L9.75 4.81l-6.286 6.287a.253.253 0 0 0-.064.108l-.558 1.953 1.953-.558a.249.249 0 0 0 .108-.064Zm1.238-3.763a.25.25 0 0 0-.354 0L10.811 3.75l1.439 1.44 1.263-1.263a.25.25 0 0 0 0-.354Z\"/></svg>")

(def x-logo
  "<svg viewBox=\"0 0 24 24\" aria-hidden=\"true\"><path d=\"M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z\"/></svg>")


(defn x-share-url
  [title url]
  (let [encode #(str/replace (java.net.URLEncoder/encode % "UTF-8") "+" "%20")]
    (str "https://x.com/intent/post?text=" (encode title) "&url=" (encode url))))


(def github-mark
  "<svg viewBox=\"0 0 16 16\" aria-hidden=\"true\"><path d=\"M6.766 11.328c-2.063-.25-3.516-1.734-3.516-3.656 0-.781.281-1.625.75-2.188-.203-.515-.172-1.609.063-2.062.625-.078 1.468.25 1.968.703.594-.187 1.219-.281 1.985-.281.765 0 1.39.094 1.953.265.484-.437 1.344-.765 1.969-.687.218.422.25 1.515.046 2.047.5.593.766 1.39.766 2.203 0 1.922-1.453 3.375-3.547 3.64.531.344.89 1.094.89 1.954v1.625c0 .468.391.734.86.547C13.781 14.359 16 11.53 16 8.03 16 3.61 12.406 0 7.984 0 3.563 0 0 3.61 0 8.031a7.88 7.88 0 0 0 5.172 7.422c.422.156.828-.125.828-.547v-1.25c-.219.094-.5.156-.75.156-1.031 0-1.64-.562-2.078-1.609-.172-.422-.36-.672-.719-.719-.187-.015-.25-.093-.25-.187 0-.188.313-.328.625-.328.453 0 .844.281 1.25.86.313.452.64.655 1.031.655s.641-.14 1-.5c.266-.265.47-.5.657-.656\"/></svg>")


(defn github-badge
  [href]
  [:a {:class "source-badge" :href href :aria-label "GitHub で原稿を開く"}
   (hiccup/raw github-mark)
   "GitHub"])


(defn source-group
  [{:keys [markdown-href org-href github-href]}]
  [:div {:class "source-group" :role "group" :aria-label "原稿"}
   [:a {:href (str "/preview" markdown-href)} ".md"]
   [:a {:href (str "/preview" org-href)} ".org"]
   [:a {:href github-href :aria-label "GitHub で原稿を開く"}
    (hiccup/raw github-mark)
    "GitHub"]])


(def lang-labels
  [["ja" "Japanese"] ["en" "English"]])


(defn lang-switch
  [lang translations]
  [:nav {:class "lang-switch" :aria-label "Language"}
   (for [[code label] lang-labels
         :when (contains? translations code)]
     (if (= code lang)
       [:span {:aria-current "page"} label]
       [:a {:href (translations code) :hreflang code} label]))])


(defn render-content
  [{:keys [title body date datetime updated updated-datetime url post? tags sources lang translations]}]
  (hiccup/html
    [:article
     [:header
      (when (seq translations) (lang-switch (or lang "ja") translations))
      [:h1 title]
      (when post?
        [:div {:class "post-meta"}
         [:div {:class "post-meta-main"}
          [:span {:class "post-dates"}
           [:span {:class "post-date" :title "公開"}
            (hiccup/raw calendar-icon) [:span {:class "visually-hidden"} "公開"] [:time {:datetime datetime} date]]
           (when (and updated (not= updated date))
             [:span {:class "post-date" :title "更新"}
              (hiccup/raw pencil-icon) [:span {:class "visually-hidden"} "更新"] [:time {:datetime updated-datetime} updated]])]
          [:a {:class "share-x" :href (x-share-url title url) :target "_blank" :rel "noopener"}
           (hiccup/raw x-logo)
           "ポスト"]]
         (when sources (source-group sources))])
      (when (seq tags)
        [:ul {:class "post-tags"}
         (for [tag tags]
           [:li [:a {:href (tag-path tag)} (str "#" tag)]])])]
     [:div {:class "content"} (hiccup/raw body)]]))


(defn source-preview-page
  [source-path {:keys [title lang draft? sources]} text]
  (let [file-name (last (str/split source-path #"/"))
        article-path (str/replace source-path #"\.(md|org)$" "")]
    {:title file-name
     :description (str "「" title "」の " file-name " の中身。")
     :lang lang
     :draft? draft?
     :preview? true
     :body (hiccup/html
             [:article
              [:header
               [:h1 file-name]
               [:div {:class "post-meta"}
                [:div {:class "post-meta-main"}
                 [:a {:class "source-badge" :href article-path} "記事に戻る"]
                 [:a {:class "source-badge" :href source-path} "そのまま開く"]
                 (github-badge (:github-href sources))]]]
              [:div {:class "content"}
               [:p "「" [:a {:href article-path} title] "」の " file-name " です。"
                "「そのまま開く」はファイルをそのまま返します。"]
               [:div {:class "feed-copy"}
                [:input {:type "text" :readonly true :value (str "curl " site-url source-path) :aria-label "取得するコマンド"}]
                [:button {:type "button" :hidden true} "コピー"]]
               [:div {:class "source-copy"}
                [:button {:type "button" :hidden true} "全文をコピー"]]
               [:pre {:class "source-text"} [:code text]]]])}))


(defn listed-posts
  [posts]
  (let [paths (set (map first posts))]
    (remove (fn [[_ page]] (contains? paths (:original page))) posts)))


(defn post-list-items
  [posts]
  [:ul
   (->> (listed-posts posts)
        (sort-by (fn [[k v]] [(:datetime v) k]))
        reverse
        (map (fn [[k v]]
               [:li
                [:time {:datetime (:datetime v)} (:date v)]
                [:a {:href k} (:title v)]
                (for [[code other] (sort (dissoc (:translations v) (or (:lang v) "ja")))]
                  [:a {:class "post-lang" :href other :hreflang code :lang code} (str/upper-case code)])
                (when (:draft? v) [:span {:class "draft-label"} "下書き"])])))])


(defn tag-pages
  [posts]
  (let [by-tag (->> posts
                    (mapcat (fn [[path page]] (map (fn [tag] [tag [path page]]) (:tags page))))
                    (group-by first)
                    (#(update-vals % (partial map second))))
        latest-date #(->> % (keep (comp :date second)) sort last)]
    (when (seq by-tag)
      (into {"/tags/index.html"
             {:title "タグ"
              :description "Conao3 Notes の記事に付けたタグの一覧。"
              :date (latest-date posts)
              :body (hiccup/html
                      [:article
                       [:header [:h1 "タグ"]]
                       [:ul {:class "tag-list"}
                        (for [[tag tagged] (sort-by (fn [[tag tagged]] [(- (count (listed-posts tagged))) tag]) by-tag)
                              :let [n (count (listed-posts tagged))]]
                          [:li [:a (cond-> {:href (tag-path tag)} (= 1 n) (assoc :class "tag-single")) (str "#" tag) [:span {:class "tag-count"} n]]])]])}}
            (map (fn [[tag tagged]]
                   [(tag-path tag)
                    {:title (str "#" tag)
                     :description (str "タグ「" tag "」を付けた記事の一覧。")
                     :date (latest-date tagged)
                     :body (hiccup/html
                             [:article
                              [:header [:h1 (str "#" tag)]]
                              [:section {:class "post-list"}
                               (post-list-items tagged)]
                              [:p [:a {:href "/tags/"} "タグの一覧へ"]]])}]))
            by-tag))))


(defn check-originals
  [pages]
  (doseq [[path {:keys [original lang title]}] pages
          :when original]
    (let [target (get pages original)]
      (when-not target
        (throw (ex-info (str "Original not found: " original) {:path path :title title})))
      (when (= (or lang "ja") (or (:lang target) "ja"))
        (throw (ex-info (str "Original has the same language: " original) {:path path :title title})))))
  pages)


(defn translation-alternates
  [{:keys [original path translations]}]
  (let [href #(subs (page-url %) (count site-url))]
    (concat (map (fn [[code p]] [code (href p)]) (sort translations))
            [["x-default" (href (or original path))]])))


(defn link-translations
  [pages]
  (let [groups (->> pages
                    (keep (fn [[path {:keys [original lang]}]]
                            (when-let [target (get pages original)]
                              {original {(or (:lang target) "ja") original
                                         (or lang "ja") path}})))
                    (apply merge-with merge))]
    (update-vals pages
                 (fn [page]
                   (if-let [translations (get groups (or (:original page) (:path page)))]
                     (assoc page :translations translations)
                     page)))))


(defn site
  ([] (site {:drafts? true}))
  ([{:keys [drafts?]}]
  (let [portfolio-langs (->> (keys portfolio/paths)
                             (filter #(or drafts? (not (get-in portfolio/pages [% :draft?]))))
                             set)
        orgs (org-subtrees (slurp "contents.org"))
        revision (git-revision)
        contents (->> (stasis/slurp-directory "generated/contents" #"\.md$")
                      (map (fn [[k v]]
                             (let [p (parse-markdown v)
                                   path (str (subs k 0 (- (count k) 3)) ".html")
                                   obj {:title (:title (:metadata p))
                                        :path path
                                        :url (page-url path)
                                        :post? (not= path "/index.html")
                                        :date (some-> (:date (:metadata p)) (subs 0 10))
                                        :datetime (some-> (:date (:metadata p)) (str/replace " " "T"))
                                        :updated (some-> (:updated (:metadata p)) (subs 0 10))
                                        :updated-datetime (some-> (:updated (:metadata p)) (str/replace " " "T"))
                                        :lang (:lang (:metadata p))
                                        :original (some-> (:original (:metadata p)) str/trim not-empty (str ".html"))
                                        :tags (parse-tags (:tags (:metadata p)))
                                        :draft? (= "true" (some-> (:draft (:metadata p)) str/lower-case))
                                        :description (:description p)
                                        :markdown? true
                                        :source (second (str/split v #"\n\n" 2))
                                        :sources (when-let [{:keys [lines]} (get orgs path)]
                                                   {:markdown-href (markdown-path path)
                                                    :org-href (org-path path)
                                                    :github-href (source-github-url revision lines)})
                                        :body (:html p)}]
                               [path obj])))
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
                      check-originals
                      (remove (fn [[_ page]] (and (:draft? page) (not drafts?))))
                      (into {})
                      link-translations
                      (#(update-vals % (fn [page] (cond-> page (:markdown? page) (assoc :body (render-content page)))))))
        portfolio-href (fn [lang]
                         (let [lang (if (= lang "en") :en :ja)]
                           (when (contains? contents (portfolio/paths lang))
                             (portfolio/urls lang))))
        blog-inx (hiccup/html
                   [:section {:class "post-list"}
                    [:h2 "Blogs"]
                    (post-list-items (->> (dissoc contents "/index.html")
                                          (remove (comp :portfolio? second))))])
        contents (-> contents
                     (update-in ["/index.html" :body] #(str % blog-inx))
                     (merge (tag-pages (filter (comp :post? second) contents)))
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
        feed-xml (atom-feed posts)
        portfolio-alternates (let [langs (filter #(contains? contents (portfolio/paths %)) [:en :ja])]
                               (when (< 1 (count langs))
                                 (concat (map (fn [lang] [(name lang) (portfolio/urls lang)]) langs)
                                         [["x-default" (portfolio/urls :en)]])))
        contents (-> contents
                     (update "/index.html" assoc
                             :description site-description
                             :date (:date (second (first posts))))
                     (assoc "/feed/index.html" (feed-page feed-xml (:date (second (first posts))))
                            sites/path (sites/page))
                     (update-vals #(cond-> %
                                     (:portfolio? %) (assoc :alternates portfolio-alternates)
                                     (:translations %) (assoc :alternates (translation-alternates %)))))]
    (stasis/merge-page-sources
      {:contents (into {} (map (fn [[path page]] [path (render-page path (assoc page :portfolio-href (portfolio-href (:lang page))))])) contents)
       :markdown (-> (into {}
                           (map (fn [[path page]] [(markdown-path path) (post-markdown page (:source page))]))
                           posts)
                     (assoc "/index.md" (index-markdown posts)
                            "/404.md" not-found-markdown))
       :org (into {}
                  (map (fn [[path page]]
                         (if-let [{:keys [text]} (get orgs path)]
                           [(org-path path) text]
                           (throw (ex-info (str "No contents.org subtree for " path) {:path path :title (:title page)})))))
                  posts)
       :previews (into {}
                       (mapcat (fn [[path page]]
                                 (for [[source-path text-fn] [[(markdown-path path) #(post-markdown page (:source page))]
                                                              [(org-path path) #(:text (get orgs path))]]]
                                   [(preview-path source-path)
                                    (fn [_]
                                      (let [preview (source-preview-page source-path page (text-fn))]
                                        (render-page (preview-path source-path)
                                                     (assoc preview :portfolio-href (portfolio-href (:lang page))))))])))
                       posts)
       :agents {"/sitemap.xml" (sitemap-xml (dissoc contents "/404.html"))
                "/robots.txt" robots-txt
                "/llms.txt" (llms-txt posts)
                "/feed.xml" feed-xml}
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
    (io/copy (io/file "resources/_headers") (io/file export-dir "_headers"))
    (println "Export complete:")
    (stasis/report-differences old-files (load-export-dir))))


(defn- wrap-extensionless
  [handler]
  (fn [req]
    (let [uri (:uri req)
          html-req (when (and (not (str/ends-with? uri "/")) (not (re-find #"\.[^/]*$" uri)))
                     (assoc req :uri (str uri ".html")))
          res (when html-req (handler html-req))]
      (if (and res (= 200 (:status res)))
        res
        (handler req)))))

(defn- wrap-source-preview
  [handler]
  (fn [req]
    (if (re-find #"^/preview/.*\.(md|org)$" (:uri req))
      (handler (update req :uri str ".html"))
      (handler req))))


(def source-mime-types
  {"md" "text/markdown; charset=utf-8"
   "org" "text/plain; charset=utf-8"})


(defn start-server
  [& _args]
  (jetty/run-jetty
    (-> (stasis/serve-pages site stasis-config)
        wrap-extensionless
        wrap-source-preview
        (file/wrap-file "resources/public" {:index-files? false})
        (content-type/wrap-content-type {:mime-types source-mime-types})
        not-modified/wrap-not-modified)
    {:port 8080 :join? false}))
