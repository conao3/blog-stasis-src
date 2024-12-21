(ns blog-clojure.core
  (:require
   [hiccup.page :as hiccup.page]
   [stasis.core :as stasis]
   [ring.adapter.jetty :as jetty]))

(def pages
  {"/index.html" {:title "Home" :content "Welcome to my static site!"}
   "/about.html" {:title "About" :content "This is a simple static site!"}})

(defn render-page [{:keys [title content]}]
  (hiccup.page/html5
    [:head
     [:title title]]
    [:body
     [:h1 title]
     [:p content]]))

(defn site []
  (stasis/merge-page-sources
    {:pages (into {}
                  (map (fn [[path data]]
                         [path (render-page data)]))
                  pages)}))

(defn export [& _args]
  (stasis/export-pages (site) "target/"))

(defn start-server [& _args]
  (jetty/run-jetty
    (stasis/serve-pages site)
    {:port 8080}))