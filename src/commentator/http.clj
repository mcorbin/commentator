(ns commentator.http
  (:require [commentator.interceptor.auth :as itc-auth]
            [commentator.interceptor.cors :as itc-cors]
            [commentator.interceptor.route :as itc-route]
            [com.stuartsierra.component :as component]
            [corbihttp.interceptor.error :as itc-error]
            [corbihttp.interceptor.id :as itc-id]
            [corbihttp.interceptor.json :as itc-json]
            [corbihttp.interceptor.ring :as itc-ring]
            [corbihttp.interceptor.response :as itc-response]
            [exoscale.interceptor :as interceptor]))

(defn interceptor-chain
  [token api-handler]
  [itc-response/response ;;leave
   itc-cors/cors ;; leave
   itc-json/json ;; leave
   itc-error/error ;; error
   itc-id/request-id ;;enter
   itc-ring/cookies ;; enter + leave
   itc-ring/params ;; enter
   itc-ring/keyword-params ;; enter
   itc-json/request-params ;; enter
   itc-route/match-route ;; enter
   (itc-auth/auth token) ;; enter
   (itc-route/route api-handler) ;; enter
   ])

(defn execute!
  [request chain]
  (interceptor/execute {:request request} chain))

(defrecord ChainHandler [token api-handler itc-chain]
  component/Lifecycle
  (start [this]
    (assoc this :itc-chain (interceptor-chain token api-handler)))
  (stop [this]
    (assoc this :it-chain nil))
  clojure.lang.IFn
  (invoke [this request] (execute! request itc-chain))
  (applyTo [this args] (clojure.lang.AFn/applyToHelper this args)))
