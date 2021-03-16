(defproject cljs-proof-of-date "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [org.clojure/clojurescript "1.10.773"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library
                               org.clojure/google-closure-library-third-party]]
                 [thheller/shadow-cljs "2.11.23"]
                 [reagent "1.0.0"]
                 [re-frame "1.2.0"]
                 [day8.re-frame/tracing "0.6.2"]
                 [day8.re-frame/http-fx "0.2.3"]
                 [re-com "2.13.2"]
                 [clj-commons/secretary "1.2.4"]
                 [garden "1.3.10"]
                 [ns-tracker "0.4.0"]
                 [compojure "1.6.2"]
                 [me.raynes/fs "1.4.6"]
                 [cheshire "5.10.0"]
                 [yogthos/config "1.1.7"]
                 [ring "1.9.1"]
                 [ring/ring-json "0.5.0"]
                 [ring-cors "0.1.13"]

                 [metabase/throttle "1.0.2"]

                 [re-pressed "0.3.1"]
                 [breaking-point "0.1.2"]

                 [org.clojure/core.async "1.3.610"]

                 [com.hedera.hashgraph/sdk "2.0.5-beta.4"]
                 [io.grpc/grpc-netty-shaded "1.36.0"
                  :exclusions [
                               com.google.errorprone/error_prone_annotations
                               ;io.grpc/grpc-core
                               ;io.grpc/grpc-api
                               ]]
                 [io.github.cdimascio/java-dotenv "5.2.2"]]

  :plugins [[lein-shadow "0.3.1"]
            [lein-garden "0.3.0"]
            [lein-shell "0.5.0"]

            [lein-ring "0.12.5"]

            [lein-vanity "0.2.0"]
            [lein-nomis-ns-graph "0.14.6"]
            [lein-ancient "0.7.0"]
            [jonase/eastwood "0.3.14"]
            [lein-kibit "0.1.8"]
            [lein-bikeshed "0.5.2"]
            [venantius/yagni "0.1.7"]
            [lein-check-namespace-decls "1.0.2"]
            [docstring-checker "1.1.0"]
            [lein-check-namespace-decls "1.0.2"]]


  :ring {:handler cljs-proof-of-date.handler/handler}

  :min-lein-version "2.9.0"

  :source-paths ["src/clj" "src/cljs"]

  :test-paths   ["test/cljs" "test/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"
                                    "test/js"
                                    "resources/public/css"]


  :garden {:builds [{:id           "screen"
                     :source-paths ["src/clj"]
                     :stylesheet   cljs-proof-of-date.css/screen
                     :compiler     {:output-to     "resources/public/css/screen.css"
                                    :pretty-print? true}}]}

  :shadow-cljs {:nrepl {:port 8777}

                :builds {:app {:target :browser
                               :output-dir "resources/public/js/compiled"
                               :asset-path "/js/compiled"
                               :modules {:app {:init-fn cljs-proof-of-date.core/init
                                               :preloads [devtools.preload
                                                          day8.re-frame-10x.preload]}}
                               :install-deps true
                               :npm-deps {:gun-avatar "1.2.3"
                                          :qr-encode "0.3.0"}

                               :dev {:compiler-options {:closure-defines {"goog.DEBUG" true
                                                                          re-frame.trace.trace-enabled? true
                                                                          day8.re-frame.tracing.trace-enabled? true}}}
                               :release {:compiler-options {:closure-defines {"goog.DEBUG" false}
                                                            ;:optimizations :whitespace
                                                            ;:pseudo-names true
                                                            ;:pretty-print true
                                                            }
                                         :build-options
                                         {:ns-aliases
                                          {day8.re-frame.tracing day8.re-frame.tracing-stubs}}}

                               :devtools {:http-root "resources/public"
                                          :http-port 8280
                                          :http-handler cljs-proof-of-date.handler/dev-handler}}
                         :browser-test
                         {:target :browser-test
                          :ns-regexp "-test$"
                          :runner-ns shadow.test.browser
                          :test-dir "target/browser-test"
                          :devtools {:http-root "target/browser-test"
                                     :http-port 8290}}

                         :karma-test
                         {:target :karma
                          :ns-regexp "-test$"
                          :output-to "target/karma-test.js"}}}

  :shell {:commands {"karma" {:windows         ["cmd" "/c" "karma"]
                              :default-command "karma"}
                     "open"  {:windows         ["cmd" "/c" "start"]
                              :macosx          "open"
                              :linux           "xdg-open"}}}

  :aliases {"dev"          ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein watch instead.\""]
                            ["watch"]]
            "watch"        ["with-profile" "dev" "do"
                            ["shadow" "watch" "app" "browser-test" "karma-test"]]

            "prod"         ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein release instead.\""]
                            ["release"]]

            "release"      ["with-profile" "prod" "do"
                            ["shadow" "release" "app"]]

            "build-report" ["with-profile" "prod" "do"
                            ["shadow" "run" "shadow.cljs.build-report" "app" "target/build-report.html"]
                            ["shell" "open" "target/build-report.html"]]

            "karma"        ["do"
                            ["shell" "echo" "\"DEPRECATED: Please use lein ci instead.\""]
                            ["ci"]]
            "ci"           ["with-profile" "prod" "do"
                            ["shadow" "compile" "karma-test"]
                            ["shell" "karma" "start" "--single-run" "--reporters" "junit,dots"]]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "1.0.2"]
                   [day8.re-frame/re-frame-10x "1.0.1"]]
    :source-paths ["dev"]}

   :prod {}

   :uberjar {:source-paths ["env/prod/clj"]
             :omit-source  true
             :main         cljs-proof-of-date.server
             :aot          [cljs-proof-of-date.server]
             :uberjar-name "cljs-proof-of-date.jar"
             :prep-tasks   ["compile" ["release"]["garden" "once"]]}}

  :prep-tasks [["garden" "once"]])
