(ns commentator.interceptor.cors
  (:require [exoscale.interceptor :as interceptor]))

(def cors
  {:name ::cors
   :enter (fn [ctx]
            (if (= :options (get-in ctx [:request :request-method]))
              (interceptor/halt {:status 200
                                 :headers {"Access-Control-Allow-Origin" "*"
                                           "Access-Control-Allow-Methods" "POST, GET, OPTIONS"
                                           "Access-Control-Allow-Headers" "Content-Type"}})
              ctx))
   :leave (fn [ctx]
            (let [headers (merge (get-in ctx [:response :headers])
                                 {"Access-Control-Allow-Origin" "*"})]
              (assoc-in ctx [:response :headers] headers)))})
