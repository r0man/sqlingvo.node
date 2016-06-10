(defproject sqlingvo.node "0.1.0"
  :description "A ClojureScript driver for SQLingvo on Node.js."
  :url "http://github.com/r0man/sqlingvo.node"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.6.1"
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.374"]
                 [sqlingvo "0.8.13"]]
  :npm {:dependencies [[pg "5.0.0"]
                       [pg-native "1.10.0"]]}
  :profiles
  {:dev
   {:dependencies [[org.clojure/test.check "0.9.0"]]
    :plugins [[jonase/eastwood "0.2.3"]
              [lein-cljsbuild "1.1.3"]
              [lein-difftest "2.0.0"]
              [lein-doo "0.1.6"]
              [lein-npm "0.6.2"]]}
   :provided
   {:dependencies [[org.clojure/clojurescript "1.9.36"]]}
   :repl
   {:dependencies [[com.cemerick/piggieback "0.2.1"]
                   [reloaded.repl "0.2.2"]]
    :plugins [[figwheel-sidecar "0.5.3-2"]]
    :init-ns user
    :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
  :aliases
  {"ci" ["do"
         ["clean"]
         ["difftest"]
         ["doo" "node" "node" "once"]
         ["lint"]]
   "lint" ["do"  ["eastwood"]]}
  :cljsbuild
  {:builds
   [{:id "node"
     :compiler
     {:main 'sqlingvo.test
      :optimizations :none
      :output-dir "target/node"
      :output-to "target/node.js"
      :parallel-build true
      :pretty-print true
      :target :nodejs
      :verbose false}
     :source-paths ["src" "test"]}]})
