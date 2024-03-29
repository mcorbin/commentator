(defproject commentator "0.18.0"
  :description "A Free commenting system"
  :url "https://github.com/mcorbin/commentator"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[amazonica "0.3.156"
                  :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core "1.12.128"]
                 [com.amazonaws/aws-java-sdk-s3 "1.12.128"]
                 [fr.mcorbin/corbihttp "0.30.0"]
                 [org.clojure/clojure "1.10.3"]
                 [org.clojure/core.cache "1.0.225"]]
  :main ^:skip-aot commentator.core
  :target-path "target/%s"
  :source-paths ["src"]
  :profiles {:dev {:dependencies [[org.clojure/tools.namespace "1.2.0"]
                                  [pjstadig/humane-test-output "0.11.0"]
                                  [tortue/spy "2.9.0"]
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
