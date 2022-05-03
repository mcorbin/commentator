(ns commentator.interceptor.auth
  (:require [corbihttp.interceptor.auth :as itc-auth]))

(defn auth
  [username password]
  {:name ::auth
   :enter (fn [ctx]
            (let [admin? (-> ctx :handler :auth)]
              (if admin?
                (itc-auth/check username password "commentator" ctx)
                ctx)))})
