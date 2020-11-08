(ns commentator.core
  (:require [com.stuartsierra.component :as component]
            [commentator.comment :as comment]
            [commentator.config :as config]
            [commentator.event :as event]
            [commentator.handler :as handler]
            [commentator.http :as http]
            [commentator.log :as log]
            [commentator.store :as store]
            [signal.handler :refer [with-handler]]
            [unilog.config :refer [start-logging!]])
  (:gen-class))

(defonce ^:redef system
  nil)

(defn build-system
  [{:keys [http admin store comment challenges]}]
  (component/system-map
   :http (-> (http/map->Server (merge http
                                      admin))
             (component/using [:handler]))
   :s3 (store/map->S3 {:credentials (dissoc store :bucket)
                       :bucket (:bucket store)})
   :event-manager (-> (event/map->EventManager {})
                      (component/using [:s3]))
   :comment-manager (-> (comment/map->CommentManager (update comment :allowed-articles set))
                        (component/using [:s3]))
   :handler (-> (handler/map->Handler {:challenges challenges})
                (component/using [:event-manager :comment-manager]))))

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
       (log/info {} "system started")
       (catch Exception e
         (System/exit 1))))
