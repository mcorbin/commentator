(ns commentator.interceptor.auth
  (:require [commentator.interceptor.route :as route]
            [exoscale.cloak :as cloak]
            [exoscale.ex :as ex]))

(defn get-auth-token
  "Takes a request. Extracts the Authorization header."
  [request]
  (get-in request [:headers "authorization"]))

(defn auth
  [token]
  {:name ::auth
   :enter (fn [ctx]
            (let [request (:request ctx)
                  admin? (route/admin-calls (:handler request))
                  request-token (get-auth-token request)]
              (when (and admin?
                         (not= (cloak/unmask token) request-token))
                (throw (ex/ex-forbidden "Forbidden" {})))
              ctx))})

