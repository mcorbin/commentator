(defproject commentator "0.4.0"
  :description "A Free commenting server"
  :url "https://github.com/mcorbin/commentator"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[amazonica "0.3.153"
                  :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [cheshire "5.10.0"]
                 [com.amazonaws/aws-java-sdk-core "1.11.913"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.913"]
                 (exoscale/coax "1.0.0-alpha10")
                 [mcorbin/corbihttp "0.16.0-SNAPSHOT"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/core.cache "1.0.207"]
                 [spootnik/signal "0.2.4"]]
  :main ^:skip-aot commentator.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.1.0"]
                                  [pjstadig/humane-test-output "0.10.0"]
                                  [tortue/spy "2.4.0"]
                                  [ring/ring-mock "0.4.0"]]
                   :global-vars    {*assert* true}
                   :env {:commentator-configuration "dev/resources/config.edn"}
                   :plugins [[lein-environ "1.1.0"]
                             [lein-cloverage "1.1.1"]
                             [lein-ancient "0.6.15"]
                             [lein-cljfmt "0.6.6"]]
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]
                   :repl-options {:init-ns user}
                   :source-paths ["dev"]}
             :uberjar {:aot :all}}
  :test-selectors {:default (fn [x] (not (:integration x)))
                   :integration :integration})
