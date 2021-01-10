(ns commentator.core
  (:require [com.stuartsierra.component :as component]
            [commentator.comment :as comment]
            [commentator.config :as config]
            [commentator.event :as event]
            [commentator.handler :as handler]
            [commentator.http :as http]
            [commentator.rate-limit :as rate-limit]
            [commentator.store :as store]
            [corbihttp.http :as corbihttp]
            [corbihttp.log :as log]
            [corbihttp.metric :as metric]
            [signal.handler :refer [with-handler]]
            [unilog.config :refer [start-logging!]])
  (:gen-class))

(defonce ^:redef system
  nil)

(defn build-system
  [{:keys [http admin store comment challenges prometheus]}]
  (let [registry (metric/registry-component {})
        prom-handler (metric/prom-handler registry)]
    (component/system-map
     :registry registry
     :http (-> (corbihttp/map->Server {:config http})
               (component/using [:handler]))
     :s3 (store/map->S3 {:credentials (dissoc store :bucket)
                         :bucket (:bucket store)})
     :rate-limiter (rate-limit/map->SimpleRateLimiter {})
     :event-manager (-> (event/map->EventManager {})
                        (component/using [:s3]))
     :comment-manager (-> (comment/map->CommentManager (update comment :allowed-articles set))
                          (component/using [:s3]))
     :prometheus (if (and prometheus (not (empty? prometheus)))
                   (corbihttp/map->Server {:config prometheus
                                           :handler prom-handler})
                   {})
     :handler (-> (http/map->ChainHandler admin)
                  (component/using [:api-handler :registry]))
     :api-handler (-> (handler/map->Handler {:challenges challenges})
                      (component/using [:event-manager :comment-manager :rate-limiter])))))

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
