(ns commentator.chain
  (:require [commentator.api :as api]
            [commentator.interceptor.auth :as itc-auth]
            [commentator.interceptor.cors :as itc-cors]
            [commentator.handler :as handler]
            [corbihttp.interceptor.error :as itc-error]
            [corbihttp.interceptor.id :as itc-id]
            [corbihttp.interceptor.json :as itc-json]
            [corbihttp.interceptor.metric :as itc-metric]
            [corbihttp.interceptor.ring :as itc-ring]
            [corbihttp.interceptor.route :as itc-route]
            [corbihttp.interceptor.handler :as itc-handler]
            [corbihttp.interceptor.response :as itc-response]))

(defn interceptor-chain
  [{:keys [username password api-handler registry allow-origin]}]
  [itc-response/response ;;leave
   itc-json/json ;; leave
   (itc-error/last-error registry) ;;error
   (itc-metric/response-metrics registry) ;; leave
   (itc-cors/cors allow-origin) ;; leave
   itc-error/error ;; error
   (itc-route/route {:router api/router
                     :registry registry
                     :handler-component api-handler
                     :not-found-handler handler/not-found}) ;; enter
   (itc-auth/auth username password)
   itc-id/request-id ;;enter
   itc-ring/cookies ;; enter + leave
   itc-ring/params ;; enter
   itc-ring/keyword-params ;; enter
   itc-json/request-params ;; enter
   (itc-handler/main-handler {:registry registry
                              :handler-component api-handler})])
