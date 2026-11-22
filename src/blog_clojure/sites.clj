(ns blog-clojure.sites
  (:require
    [hiccup2.core :as hiccup]))


(def path "/sites/index.html")


(def url "/sites/")


(def sites
  [{:name "Elispore"
    :host "elispore.conao3.com"
    :lang "en"
    :text "GitHub にある Emacs のパッケージを探すサイト。定期的に更新するカタログを検索し、そのままソースへ移れます。"
    :source "https://github.com/conao3/ts-elispore"}
   {:name "GNU Emacs マニュアル (機械翻訳)"
    :host "emacs-doc.conao3.com"
    :lang "ja"
    :text "GNU Emacs に同梱のマニュアルを PLaMo 翻訳で日本語に機械翻訳したもの。Emacs の開発版 (master) に合わせて毎日更新します。"
    :source "https://github.com/conao3/emacs-doc"}
   {:name "ritsulint"
    :host "ritsulint.conao3.com"
    :lang "ja"
    :text "日本の法令の linter。e-Gov 法令検索の法令 XML を読み、読替えの引用・位置で指す参照・番号・目次が条文と合わない箇所を一覧にします。"
    :source "https://github.com/conao3/rust-ritsulint"}
   {:name "deva"
    :host "deva.conao3.com"
    :lang "en"
    :text "トピックごとに 1 冊の本として書く、個人の知識ベース。英語の語彙、Org Mode、pstack などのガイドを置いています。"}
   {:name "Cloudflare Agents samples"
    :host "cloudflare-agents-sample.conao3.com"
    :lang "en"
    :text "Cloudflare Agents のプラットフォームで作ったサンプルを並べたダッシュボード。"
    :source "https://github.com/conao3/cloudflare-agents-sample"}])


(defn page
  []
  {:title "サイト"
   :description "conao3.com のサブドメインで公開しているサイトの一覧。"
   :body (hiccup/html
           [:article
            [:header [:h1 "サイト"]]
            [:div {:class "content"}
             [:p "このブログのほかに、conao3.com のサブドメインで公開しているサイトです。"]
             [:ul {:class "site-list"}
              (for [{:keys [name host lang text source]} sites]
                [:li
                 [:a {:href (str "https://" host "/") :lang lang} [:strong name]]
                 [:span {:class "site-host"} host]
                 [:p text]
                 (when source
                   [:p {:class "site-source"} [:a {:href source} "ソースコード"]])])]]])})
