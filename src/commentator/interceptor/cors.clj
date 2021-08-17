(ns commentator.interceptor.cors
  (:require [exoscale.interceptor :as interceptor]))

(defn get-origin
  "Returns the value to set to the Access-Control-Allow-Origin header, or the
  random value from allow-origin if the request Origin is lot allowed"
  [request allow-origin]
  (or (allow-origin (get-in request [:headers "origin"]))
      (first allow-origin)))

(defn cors
  [allow-origin]
  {:name ::cors
   :enter (fn [ctx]
            (if (= :options (get-in ctx [:request :request-method]))
              (interceptor/halt {:status 200
                                 :headers {"Access-Control-Allow-Origin"
                                           (get-origin (:request ctx) allow-origin)
                                           "Vary" "Origin"
                                           "Access-Control-Allow-Methods" "POST, GET, OPTIONS"
                                           "Access-Control-Allow-Headers" "Content-Type"}})
              ctx))
   :leave (fn [ctx]
            (let [headers (merge (get-in ctx [:response :headers])
                                 {"Vary" "Origin"
                                  "Access-Control-Allow-Origin"
                                  (get-origin (:request ctx) allow-origin)})]
              (assoc-in ctx [:response :headers] headers)))})
