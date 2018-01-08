(defproject sqlingvo.node "0.2.2-SNAPSHOT"
  :description "A ClojureScript driver for SQLingvo on Node.js."
  :url "http://github.com/r0man/sqlingvo.node"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.6.1"
  :deploy-repositories [["releases" :clojars]]
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/core.async "0.3.465"]
                 [sqlingvo "0.9.17"]]
  :profiles
  {:dev
   {:dependencies [[org.clojure/test.check "0.9.0"]]
    :plugins [[jonase/eastwood "0.2.5"]
              [lein-cljsbuild "1.1.7"]
              [lein-difftest "2.0.0"]
              [lein-doo "0.1.8"]]}
   :provided
   {:dependencies [[org.clojure/clojurescript "1.9.946"]]}
   :repl
   {:dependencies [[com.cemerick/piggieback "0.2.2"]]
    :plugins [[figwheel-sidecar "0.5.14"]]
    :init-ns user
    :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}
  :aliases
  {"ci" ["do"
         ["clean"]
         ["difftest"]
         ["doo" "node" "node" "once"]
         ["lint"]]
   "lint" ["do"  ["eastwood"]]}
  :clean-targets
  [".cljs_node_repl"
   "node_modules"
   "out"
   "package-lock.json"
   "package.json"]
  :cljsbuild
  {:builds
   [{:id "node"
     :compiler
     {:main 'sqlingvo.node.test
      :npm-deps
      {:pg "7.4.1"
       :pg-native "2.2.0"}
      :install-deps true
      :optimizations :none
      :output-dir "target/node"
      :output-to "target/node.js"
      :parallel-build true
      :pretty-print true
      :target :nodejs
      :verbose false}
     :source-paths ["src" "test"]}]})
