(ns commentator.http
  (:require [com.stuartsierra.component :as component]
            [commentator.interceptor.auth :as itc-auth]
            [commentator.interceptor.error :as itc-error]
            [commentator.interceptor.id :as itc-id]
            [commentator.interceptor.json :as itc-json]
            [commentator.interceptor.ring :as itc-ring]
            [commentator.interceptor.response :as itc-response]
            [commentator.interceptor.route :as itc-route]
            [exoscale.interceptor :as interceptor]
            [ring.adapter.jetty :as jetty]))

(defn interceptor-handler
  [token]
  (let [interceptors
        [itc-response/response ;;leave
         itc-json/json ;; leave
         itc-error/error ;; error
         itc-id/request-id ;;enter
         itc-ring/cookies ;; enter + leave
         itc-ring/params ;; enter
         itc-ring/keyword-params ;; enter
         itc-route/match-route ;; enter
         (itc-auth/auth token)
         itc-route/route ;; enter
         ]]
    (fn handler [request]
      (interceptor/execute {:request request} interceptors))))

(defrecord Server [host port server token]
  component/Lifecycle
  (start [this]
    (assoc this :server
           (jetty/run-jetty (interceptor-handler token))))
  (stop [this])
  )
