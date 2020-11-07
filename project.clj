(defproject commentator "0.1.0-SNAPSHOT"
  :description "A Free commenting server"
  :url "https://github.com/mcorbin/commentator"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[amazonica "0.3.153"
                  :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [bidi "2.1.6"]
                 [cheshire "5.10.0"]
                 [com.amazonaws/aws-java-sdk-core "1.11.882"]
                 [com.amazonaws/aws-java-sdk-s3 "1.11.882"]
                 [com.stuartsierra/component "1.0.0"]
                 [commons-codec/commons-codec "1.15"]
                 [exoscale/cloak "0.1.3"]
                 (exoscale/coax "1.0.0-alpha10")
                 [exoscale/ex "0.3.15"]
                 [exoscale/interceptor "0.1.9"]
                 [io.micrometer/micrometer-registry-prometheus "1.5.5"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.logging "1.1.0"]
                 [ring/ring-core "1.8.2"]
                 [ring/ring-jetty-adapter "1.8.2"]
                 [spootnik/signal "0.2.4"]
                 [spootnik/unilog "0.7.25"]]
  :main ^:skip-aot commentator.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.0.0"]
                                  [pjstadig/humane-test-output "0.10.0"]
                                  [tortue/spy "2.0.0"]
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
