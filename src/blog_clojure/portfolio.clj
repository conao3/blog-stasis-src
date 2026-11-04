(ns blog-clojure.portfolio
  (:require
    [hiccup2.core :as hiccup]))


(def title "Naoya Yamashita")


(def description
  "Backend-focused software engineer with 6+ years building and operating production services in TypeScript, Python, and Clojure on PostgreSQL and AWS.")


(def links
  [["GitHub" "https://github.com/conao3"]
   ["LinkedIn" "https://www.linkedin.com/in/conao3/"]
   ["X" "https://x.com/conao_3"]
   ["Tech Book Fest" "https://techbookfest.org/organization/b48gui73eygdZDs02kqB1C"]
   ["Email" "mailto:conao3@gmail.com"]])


(def open-source
  [["leaf.el" "https://github.com/conao3/leaf.el"
    "Declarative init.el configuration for Emacs. On ELPA and MELPA with 5 companion packages."]
   ["chrome-devtools" "https://github.com/conao3/rust-chrome-devtools"
    "Rust CLI that brokers Chrome DevTools Protocol (CDP) sessions across coding agents, with a per-profile daemon and JSON-RPC batches. Published on crates.io."]
   ["feather.el" "https://github.com/conao3/feather.el"
    "Parallel Emacs package manager with async installs."]
   ["seml-mode.el" "https://github.com/conao3/seml-mode.el"
    "Major mode for SEML, HTML authoring in S-expressions."]
   ["keg.el" "https://github.com/conao3/keg.el"
    "Elisp package development tooling with built-in testing and CI."]])


(def maintained
  [["cask" "https://github.com/cask/cask" "project management for Emacs"]
   ["ivy-posframe" "https://github.com/tumashu/ivy-posframe" "ivy completion in a child frame"]
   ["skkeleton" "https://github.com/vim-skk/skkeleton" "SKK input method for Vim/Neovim"]])


(def experience
  [{:role "Software Engineer"
    :company "Toyokumo, Inc."
    :period "Jun 2024 – Present (on leave)"
    :items ["Full-stack development of \"kMailer,\" a kintone-integrated SaaS email delivery service: Clojure GraphQL API on PostgreSQL, ClojureScript and Next.js/React (TypeScript) frontend, deployed on AWS ECS."
            "Designed and shipped 10+ roadmap features (HTML block editor, scenario-based email automation, delivery analytics, A/B testing), writing design docs to align product, design, and customer success."
            "Raised the team's Release per Business Day from 0.8 to 2.1 by standardizing deployment, 1st among 6 teams. Merged 334 pull requests in 6 months (55/month), the most on the team."
            "Led adoption of AI coding agents (Devin, Claude Code); zero-downtime migration from AWS Elastic Beanstalk to ECS (Docker), JVM 11 to 21 upgrade, and a 30% cut in GraphQL Codegen output."]}
   {:role "Software Engineer, Backend Team Lead"
    :company "Optarc LLC"
    :period "Apr 2021 – May 2024"
    :items ["Owned an automated ordering system on AWS from requirements to operations: order quantities computed from 10M+ sales records covering the previous 30 days across 100 stores."
            "Serverless microservices in Python on AWS Lambda with GraphQL APIs: Amazon Redshift for nightly batches, Aurora PostgreSQL for daytime OLTP, and Cedar policy-based authorization across 20+ endpoints."
            "Led a backend team of up to 4 engineers with test-driven development (TDD), code review, and CI/CD; SQL tuning and metrics monitoring kept the system at 99.9% uptime."]}
   {:role "Contract Frontend Engineer"
    :company "Forcode Inc."
    :period "Jan 2022 – Jun 2022"
    :items ["Built the React (TypeScript) frontend of Sony Music's internal platform for artist master data and CD/record sales."]}
   {:role "Part-time Software Engineer"
    :company "Optarc LLC"
    :period "Sep 2020 – Mar 2021"
    :items ["Set up CI/CD (CircleCI) with automated tests for a 3-person team and lifted test coverage past 60% by wrapping side-effect-heavy code in end-to-end tests before refactoring."]}
   {:role "Software Engineering Intern"
    :company "pixiv Inc."
    :period "Sep 2020 (2 weeks)"
    :items ["Migrated the pixiv Encyclopedia PHP application (100,000+ monthly users) to PSR-7/15/17 standards, turning 15+ global-state modules into unit-testable functions."]}])


(def skills
  [["Languages" "TypeScript / JavaScript, Python, Clojure, Rust, SQL"]
   ["Backend" "Node.js, REST and GraphQL APIs, microservices, serverless, OAuth 2.0 / JWT, Cedar, Amazon Cognito"]
   ["Databases" "PostgreSQL (Aurora), Redis, DynamoDB, Amazon Redshift, data modeling, query tuning"]
   ["Cloud and DevOps" "AWS (Lambda, ECS, EKS, S3, RDS, SQS, EventBridge, CloudFront, IAM), AWS CDK, Docker, Kubernetes, GitHub Actions, Nix"]
   ["Frontend" "React, Next.js, ClojureScript, TanStack Query, Apollo Client, Tailwind CSS"]
   ["AI Engineering" "Model Context Protocol (MCP), AI coding agents (Claude Code, Codex, Devin)"]])


(def talks
  [["2025年からを生きるためのEmacs入門 (Tokyo Emacs Study Meeting, Aug 2024)" "/blog/2024/d27077"]
   ["Clojureで始めるTree-sitter (Tokyo Emacs Study Meeting, Oct 2024)" "/blog/2024/5d087f"]])


(def education
  ["Hiroshima University, B.Eng., Faculty of Engineering (Apr 2015 – Mar 2020)"
   "AWS Certified Solutions Architect – Associate (Jun 2026)"
   "Applied Information Technology Engineer (IPA, Dec 2018)"])


(defn body
  []
  (str
    (hiccup/html
      [:article {:class "portfolio"}
       [:header
        [:img {:class "portfolio-icon" :src "/assets/icon.jpg" :alt "" :width 96 :height 96}]
        [:div
         [:h1 title]
         [:p {:class "portfolio-role"} "Software Engineer (Backend / Full-Stack) · conao3"]
         [:ul {:class "portfolio-links"}
          (for [[label href] links]
            [:li [:a {:href href} label]])]]]
       [:div {:class "content"}
        [:p description " Owns features end to end: API design through implementation and operation, data modeling, the React frontend, CI/CD, and on-call. Writes Rust and Emacs Lisp in open source and runs an AI-native workflow with coding agents."]
        [:h2 "Open Source"]
        [:ul
         (for [[name href text] open-source]
           [:li [:a {:href href} [:strong name]] ": " text])
         [:li "Maintainer on "
          (interpose ", "
                     (for [[name href text] maintained]
                       (list [:a {:href href} [:strong name]] " (" text ")")))
          "."]]
        [:h2 "Experience"]
        (for [{:keys [role company period items]} experience]
          (list
            [:h3 role " · " company]
            [:p {:class "portfolio-period"} period]
            [:ul (for [item items] [:li item])]))
        [:h2 "Skills"]
        [:ul
         (for [[label text] skills]
           [:li [:strong label] ": " text])]
        [:h2 "Writing and Talks"]
        [:ul
         [:li "Published 3 Clojure books at " [:a {:href "https://techbookfest.org/organization/b48gui73eygdZDs02kqB1C"} "Tech Book Fest"] ", and organize Shibuya.lisp."]
         (for [[text href] talks]
           [:li [:a {:href href} text]])
         [:li [:a {:href "/"} "Blog posts (Japanese)"]]]
        [:h2 "Education and Certifications"]
        [:ul (for [item education] [:li item])]]])))
