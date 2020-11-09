(ns commentator.interceptor.ring
  (:require [exoscale.interceptor :as interceptor]
            [ring.middleware.cookies :as cookies]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as keyword-params]))

(def keyword-params
  {:name ::keyword-params
   :enter (fn [ctx] (update ctx :request keyword-params/keyword-params-request))})

(def params
  {:name ::params
   :enter (fn [ctx] (update ctx :request params/params-request))})

(def cookies
  {:name ::cookies
   :enter (fn [ctx] (update ctx :request #(cookies/cookies-request % {})))
   :leave (fn [ctx] (update ctx :response #(cookies/cookies-response % {})))})

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
