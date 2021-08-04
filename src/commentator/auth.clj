(ns commentator.auth
  (:require [exoscale.cloak :as cloak]
            [exoscale.ex :as ex]))

(def admin-calls #{:comment/get :comment/approve :comment/delete-article
                   :comment/delete :comment/admin-for-article :event/list
                   :event/delete})

(defn get-auth-token
  "Takes a request. Extracts the Authorization header."
  [request]
  (get-in request [:headers "authorization"]))

(defn auth-handler
  [ctx token]
  (let [admin? (admin-calls (:handler ctx))
        request-token (get-auth-token (:request ctx))]
    (when (and admin?
               (not= (cloak/unmask token) request-token))
      (throw (ex/ex-info "Forbidden"
                         [::forbidden [:corbi/user ::ex/forbidden]])))
    ctx))
