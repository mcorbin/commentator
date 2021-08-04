(ns commentator.interceptor.auth
  (:require [commentator.auth :as auth]))

(defn auth
  [token]
  {:name ::auth
   :enter (fn [ctx]
            (auth/auth-handler ctx token))})
