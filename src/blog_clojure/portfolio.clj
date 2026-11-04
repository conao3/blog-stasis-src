(ns blog-clojure.portfolio
  (:require
    [hiccup2.core :as hiccup]))


(def paths
  {:en "/portfolio/index.html"
   :ja "/ja/portfolio/index.html"})


(def urls
  {:en "/portfolio/"
   :ja "/ja/portfolio/"})


(def tech-book-fest "https://techbookfest.org/organization/b48gui73eygdZDs02kqB1C")


(def pages
  {:en
   {:title "Naoya Yamashita"
    :role "Software Engineer (Backend / Full-Stack) · conao3"
    :description "Backend-focused software engineer with 6+ years building and operating production services in TypeScript, Python, and Clojure on PostgreSQL and AWS."
    :summary " Owns features end to end: API design through implementation and operation, data modeling, the React frontend, CI/CD, and on-call. Writes Rust and Emacs Lisp in open source and runs an AI-native workflow with coding agents."
    :switch ["日本語" :ja]
    :links [["GitHub" "https://github.com/conao3"]
            ["LinkedIn" "https://www.linkedin.com/in/conao3/"]
            ["X" "https://x.com/conao_3"]
            ["Tech Book Fest" tech-book-fest]
            ["Email" "mailto:conao3@gmail.com"]]
    :headings {:open-source "Open Source"
               :experience "Experience"
               :skills "Skills"
               :writing "Writing and Talks"
               :education "Education and Certifications"}
    :open-source [["leaf.el" "https://github.com/conao3/leaf.el"
                   "Declarative init.el configuration for Emacs. On ELPA and MELPA with 5 companion packages." "556"]
                  ["chrome-devtools" "https://github.com/conao3/rust-chrome-devtools"
                   "Rust CLI that brokers Chrome DevTools Protocol (CDP) sessions across coding agents, with a per-profile daemon and JSON-RPC batches. Published on crates.io." nil]
                  ["feather.el" "https://github.com/conao3/feather.el"
                   "Parallel Emacs package manager with async installs." "94"]
                  ["seml-mode.el" "https://github.com/conao3/seml-mode.el"
                   "Major mode for SEML, HTML authoring in S-expressions." "79"]
                  ["keg.el" "https://github.com/conao3/keg.el"
                   "Elisp package development tooling with built-in testing and CI." "33"]]
    :maintained ["Maintainer"
                 [["cask" "https://github.com/cask/cask" "project management for Emacs" "1.3k"]
                  ["ivy-posframe" "https://github.com/tumashu/ivy-posframe" "ivy completion in a child frame" "422"]
                  ["skkeleton" "https://github.com/vim-skk/skkeleton" "SKK input method for Vim/Neovim" "335"]]]
    :experience [{:heading ["Software Engineer" "Toyokumo, Inc."]
                  :period "Jun 2024 – Present (on leave)"
                  :items ["Full-stack development of \"kMailer,\" a kintone-integrated SaaS email delivery service: Clojure GraphQL API on PostgreSQL, ClojureScript and Next.js/React (TypeScript) frontend, deployed on AWS ECS."
                          "Designed and shipped 10+ roadmap features (HTML block editor, scenario-based email automation, delivery analytics, A/B testing), writing design docs to align product, design, and customer success."
                          "Raised the team's Release per Business Day from 0.8 to 2.1 by standardizing deployment, 1st among 6 teams. Merged 334 pull requests in 6 months (55/month), the most on the team."
                          "Led adoption of AI coding agents (Devin, Claude Code); zero-downtime migration from AWS Elastic Beanstalk to ECS (Docker), JVM 11 to 21 upgrade, and a 30% cut in GraphQL Codegen output."]}
                 {:heading ["Software Engineer, Backend Team Lead" "Optarc LLC"]
                  :period "Apr 2021 – May 2024"
                  :items ["Owned an automated ordering system on AWS from requirements to operations: order quantities computed from 10M+ sales records covering the previous 30 days across 100 stores."
                          "Serverless microservices in Python on AWS Lambda with GraphQL APIs: Amazon Redshift for nightly batches, Aurora PostgreSQL for daytime OLTP, and Cedar policy-based authorization across 20+ endpoints."
                          "Led a backend team of up to 4 engineers with test-driven development (TDD), code review, and CI/CD; SQL tuning and metrics monitoring kept the system at 99.9% uptime."]}
                 {:heading ["Contract Frontend Engineer" "Forcode Inc."]
                  :period "Jan 2022 – Jun 2022"
                  :items ["Built the React (TypeScript) frontend of Sony Music's internal platform for artist master data and CD/record sales."]}
                 {:heading ["Part-time Software Engineer" "Optarc LLC"]
                  :period "Sep 2020 – Mar 2021"
                  :items ["Set up CI/CD (CircleCI) with automated tests for a 3-person team and lifted test coverage past 60% by wrapping side-effect-heavy code in end-to-end tests before refactoring."]}
                 {:heading ["Software Engineering Intern" "pixiv Inc."]
                  :period "Sep 2020 (2 weeks)"
                  :items ["Migrated the pixiv Encyclopedia PHP application (100,000+ monthly users) to PSR-7/15/17 standards, turning 15+ global-state modules into unit-testable functions."]}]
    :skills [["Languages" "TypeScript / JavaScript, Python, Clojure, Rust, SQL"]
             ["Backend" "Node.js, REST and GraphQL APIs, microservices, serverless, OAuth 2.0 / JWT, Cedar, Amazon Cognito"]
             ["Databases" "PostgreSQL (Aurora), Redis, DynamoDB, Amazon Redshift, data modeling, query tuning"]
             ["Cloud and DevOps" "AWS (Lambda, ECS, EKS, S3, RDS, SQS, EventBridge, CloudFront, IAM), AWS CDK, Docker, Kubernetes, GitHub Actions, Nix"]
             ["Frontend" "React, Next.js, ClojureScript, TanStack Query, Apollo Client, Tailwind CSS"]
             ["AI Engineering" "Model Context Protocol (MCP), AI coding agents (Claude Code, Codex, Devin)"]]
    :books ["Published 3 Clojure books at " [:a {:href tech-book-fest} "Tech Book Fest"] ", and organize Shibuya.lisp."]
    :talks [["2025年からを生きるためのEmacs入門 (Tokyo Emacs Study Meeting, Aug 2024)" "/blog/2024/d27077"]
            ["Clojureで始めるTree-sitter (Tokyo Emacs Study Meeting, Oct 2024)" "/blog/2024/5d087f"]
            ["Blog posts (Japanese)" "/"]]
    :education ["Hiroshima University, B.Eng., Faculty of Engineering (Apr 2015 – Mar 2020)"
                "AWS Certified Solutions Architect – Associate (Jun 2026)"
                "Applied Information Technology Engineer (IPA, Dec 2018)"]}

   :ja
   {:title "山下 直哉"
    :role "ソフトウェアエンジニア (バックエンド / フルスタック) · conao3"
    :description "バックエンドサービスとフルスタック Web アプリケーションの開発で 6 年以上の実務経験を持つソフトウェアエンジニア。"
    :summary "TypeScript (Node.js)、Python、Clojure、Rust を使い、AWS 上で REST / GraphQL API の設計から実装と運用、PostgreSQL のデータモデリング、React のフロントエンドまで、機能を一貫して担当できます。新しい技術スタックの習得が速く、Clojure に移って半年でチームの相談役になりました。OSS では Rust と Emacs Lisp を書いています。また、Claude Code、Codex、Devin を使った AI ネイティブな開発フローを実践しています。"
    :switch ["English" :en]
    :links [["GitHub" "https://github.com/conao3"]
            ["LinkedIn" "https://www.linkedin.com/in/conao3/"]
            ["X" "https://x.com/conao_3"]
            ["技術書典" tech-book-fest]
            ["メール" "mailto:conao3@gmail.com"]]
    :headings {:open-source "OSS 活動"
               :experience "職務経歴"
               :skills "テクニカルスキル"
               :writing "執筆・登壇"
               :education "学歴・資格"}
    :open-source [["leaf.el" "https://github.com/conao3/leaf.el"
                   "Emacs の宣言的な init.el 設定パッケージ。ELPA と MELPA で公開し、関連パッケージが 5 つあります。" "556"]
                  ["chrome-devtools" "https://github.com/conao3/rust-chrome-devtools"
                   "複数のコーディングエージェントが使う Chrome DevTools Protocol (CDP) のセッションを取り次ぐ Rust 製の CLI。プロファイルごとのデーモンと JSON-RPC のバッチで動きます。crates.io で公開しています。" nil]
                  ["feather.el" "https://github.com/conao3/feather.el"
                   "パッケージを並列に非同期でインストールする Emacs のパッケージマネージャ。" "94"]
                  ["seml-mode.el" "https://github.com/conao3/seml-mode.el"
                   "HTML を S 式で書く SEML のメジャーモード。" "79"]
                  ["keg.el" "https://github.com/conao3/keg.el"
                   "テストと CI を備えた Elisp パッケージの開発ツール。" "33"]]
    :maintained ["メンテナ"
                 [["cask" "https://github.com/cask/cask" "Emacs のプロジェクト管理" "1.3k"]
                  ["ivy-posframe" "https://github.com/tumashu/ivy-posframe" "ivy の候補を子フレームに表示" "422"]
                  ["skkeleton" "https://github.com/vim-skk/skkeleton" "Vim / Neovim の SKK 日本語入力" "335"]]]
    :experience [{:heading ["トヨクモ株式会社" "ソフトウェアエンジニア"]
                  :period "2024 年 6 月 〜 現在 (休職中)"
                  :items ["kintone 連携メール配信 SaaS「kMailer」のフルスタック開発 (Clojure の GraphQL API と PostgreSQL、ClojureScript と Next.js/React (TypeScript)、AWS ECS)。"
                          "デザインドックを書いて PM、デザイナ、CS と合意を取り、HTML ブロックエディタ、シナリオ配信、配信分析、A/B テストなど 10 以上の機能を設計して実装。"
                          "デプロイの手順を揃え、チームのリリース頻度 (Release per Business Day) を 0.8 から 2.1 へ改善し、社内 6 チーム中 1 位。半年で 334 PR (55 PR/月) をマージし、チーム内最多。"
                          "コーディングエージェント (Devin、Claude Code) のチームへの導入を主導。AWS Elastic Beanstalk から ECS (Docker) への無停止移行、JVM 11 から 21 への更新、GraphQL Codegen の生成量の 30% 削減も実施。"]}
                 {:heading ["オプターク合同会社" "ソフトウェアエンジニア / バックエンドチームリーダー"]
                  :period "2021 年 4 月 〜 2024 年 5 月"
                  :items ["100 店舗向けの自動発注システムを要件定義から運用まで担当。直近 30 日分、1,000 万件以上の販売実績から発注数を算出。"
                          "Python の AWS Lambda と GraphQL API でサーバーレスのマイクロサービスを構築。夜間バッチに Amazon Redshift、日中の処理に Aurora PostgreSQL を使い、20 以上のエンドポイントの認可を Cedar のポリシーで実装。"
                          "最大 4 人のバックエンドチームをリードし、TDD、コードレビュー、CI/CD で開発。SQL のチューニングとメトリクスの監視で稼働率 99.9% を維持。"]}
                 {:heading ["フォーコード株式会社" "業務委託 (フロントエンド)"]
                  :period "2022 年 1 月 〜 2022 年 6 月"
                  :items ["アーティストのマスタデータと CD・レコードの販売を扱う、ソニーミュージックの社内基盤で、React (TypeScript) のフロントエンドを開発。"]}
                 {:heading ["オプターク合同会社" "アルバイト"]
                  :period "2020 年 9 月 〜 2021 年 3 月"
                  :items ["3 人のチームに CircleCI の CI/CD と自動テストを導入。副作用の多いコードを E2E テストで囲んでからリファクタリングし、テストカバレッジを 60% 以上に向上。"]}
                 {:heading ["ピクシブ株式会社" "インターン"]
                  :period "2020 年 9 月 (2 週間)"
                  :items ["月間 10 万人以上が使うピクシブ百科事典の PHP アプリケーションを PSR-7/15/17 に沿って移行し、グローバル状態に依存する 15 以上のモジュールを単体テストできる関数に整理。"]}]
    :skills [["言語" "TypeScript / JavaScript、Python、Clojure、Rust、SQL"]
             ["バックエンド" "Node.js、REST / GraphQL API の設計と実装、マイクロサービス、サーバーレス、OAuth 2.0 / JWT、Cedar、Amazon Cognito"]
             ["データベース" "PostgreSQL (Aurora)、Redis、DynamoDB、Amazon Redshift、データモデリング、クエリチューニング"]
             ["クラウド / DevOps" "AWS (Lambda、ECS、EKS、S3、RDS、SQS、EventBridge、CloudFront、IAM)、AWS CDK、Docker、Kubernetes、GitHub Actions、Nix"]
             ["フロントエンド" "React、Next.js、ClojureScript、TanStack Query、Apollo Client、Tailwind CSS"]
             ["AI エンジニアリング" "Model Context Protocol (MCP)、AI コーディングエージェント (Claude Code、Codex、Devin)"]]
    :books [[:a {:href tech-book-fest} "技術書典"] "で Clojure の本を 3 冊頒布。Shibuya.lisp を運営。"]
    :talks [["2025年からを生きるためのEmacs入門 (東京Emacs勉強会、2024 年 8 月)" "/blog/2024/d27077"]
            ["Clojureで始めるTree-sitter (東京Emacs勉強会、2024 年 10 月)" "/blog/2024/5d087f"]
            ["ブログの記事一覧" "/"]]
    :education ["広島大学 工学部 卒業、学士 (工学) (2015 年 4 月 〜 2020 年 3 月)"
                "AWS Certified Solutions Architect – Associate (2026 年 6 月)"
                "応用情報技術者 (IPA、2018 年 12 月)"]}})


(defn body
  [lang]
  (let [{:keys [title role description summary switch links headings open-source maintained
                experience skills books talks education]} (pages lang)
        [switch-label switch-lang] switch
        [maintainer projects] maintained]
    (str
      (hiccup/html
        [:article {:class "portfolio"}
         [:header
          [:img {:class "portfolio-icon" :src "/assets/icon.jpg" :alt "" :width 96 :height 96}]
          [:div
           [:h1 title]
           [:p {:class "portfolio-role"} role]
           [:ul {:class "portfolio-links"}
            (for [[label href] links]
              [:li [:a {:href href} label]])
            [:li [:a {:href (urls switch-lang) :hreflang (name switch-lang) :lang (name switch-lang)} switch-label]]]]]
         [:div {:class "content"}
          [:p description summary]
          [:h2 (:open-source headings)]
          [:ul
           (for [[project href text stars] open-source]
             [:li [:a {:href href} [:strong project]] (when stars (str " (★ " stars ")")) ": " text])
           [:li maintainer
            [:ul
             (for [[project href text stars] projects]
               [:li [:a {:href href} [:strong project]] " (★ " stars "): " text])]]]
          [:h2 (:experience headings)]
          (for [{:keys [heading period items]} experience]
            (list
              [:h3 (first heading) " · " (second heading)]
              [:p {:class "portfolio-period"} period]
              [:ul (for [item items] [:li item])]))
          [:h2 (:skills headings)]
          [:ul
           (for [[label text] skills]
             [:li [:strong label] ": " text])]
          [:h2 (:writing headings)]
          [:ul
           [:li (seq books)]
           (for [[text href] talks]
             [:li [:a {:href href} text]])]
          [:h2 (:education headings)]
          [:ul (for [item education] [:li item])]]]))))
