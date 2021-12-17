(ns commentator.core
  (:require [com.stuartsierra.component :as component]
            [commentator.cache :as c]
            [commentator.comment :as comment]
            [commentator.config :as config]
            commentator.challenge
            [commentator.event :as event]
            [commentator.handler :as handler]
            [commentator.chain :as chain]
            [commentator.lock :as lock]
            [commentator.rate-limit :as rate-limit]
            [commentator.store :as store]
            [corbihttp.http :as corbihttp]
            [corbihttp.log :as log]
            [corbihttp.metric :as metric]
            [signal.handler :refer [with-handler]]
            [unilog.config :refer [start-logging!]])
  (:import commentator.challenge.ChallengeManager)
  (:gen-class))

(defonce ^:redef system
  nil)

(defn allowed-articles-set
  [comment-config]
  (if (:allowed-articles comment-config)
    (update comment-config
            :allowed-articles
            #(when %
               (into {}
                     (for [[k v] %]
                       [(name k) (set v)]))))
    comment-config))

(defn build-system
  [{:keys [http
           admin
           store
           comment
           challenges
           prometheus
           rate-limit-minutes
           allow-origin]}]
  (let [registry (metric/registry-component {})
        challenges (assoc challenges :key-spec
                          true)]
    (component/system-map
     :lock (lock/map->Lock {})
     :registry registry
     :http (-> (corbihttp/map->Server (merge {:config http
                                              :chain-builder chain/interceptor-chain
                                              :registry registry
                                              :allow-origin (set allow-origin)}
                                             admin))
               (component/using [:api-handler]))
     :s3 (store/map->S3 {:credentials (dissoc store :bucket)
                         :bucket-prefix (:bucket-prefix store)})
     :rate-limiter (rate-limit/map->SimpleRateLimiter {:rate-limit-minutes rate-limit-minutes})
     :challenge-manager (ChallengeManager. challenges)
     :event-manager (-> (event/map->EventManager {})
                        (component/using [:s3 :lock]))
     :comment-manager (-> (comment/map->CommentManager (allowed-articles-set
                                                        comment))
                          (component/using [:s3 :cache :lock]))
     :prometheus (if (and prometheus (seq prometheus))
                   (corbihttp/map->Server {:config prometheus
                                           :registry registry
                                           :chain-builder metric/prom-chain-builder})
                   {})
     :cache (c/map->MemoryCache {})
     :api-handler (-> (handler/map->Handler {:challenges challenges})
                      (component/using [:event-manager :comment-manager :challenge-manager :rate-limiter])))))

(defn init-system
  "Initialize system, dropping the previous state."
  [config]
  (let [sys (build-system config)]
    (alter-var-root #'system (constantly sys))))

(defn stop!
  "Stop the system."
  []
  (let [sys (component/stop-system system)]
    (alter-var-root #'system (constantly sys))))

(defn start!
  "Start the system."
  []
  (try
    (let [config (config/load-config)
          _ (start-logging! (:logging config))
          _ (init-system config)
          sys (component/start-system system)]
      (alter-var-root #'system (constantly sys)))
    (catch Exception e
      (log/error {} e "fail to start the system")
      (throw e))))

(defn -main
  "Starts the application"
  [& args]
  (with-handler :term
    (log/info {} "SIGTERM, stopping")
    (stop!)
    (log/info {} "the system is stopped")
    (System/exit 0))

  (with-handler :int
    (log/info {} "SIGINT, stopping")
    (stop!)
    (log/info {} "the system is stopped")
    (System/exit 0))
  (try (start!)
       (log/info {} "Mais y connaît pas Raoul ce mec ! Y va avoir un réveil pénible...")
       (log/info {} "system started")
       (catch Exception e
         (System/exit 1))))
