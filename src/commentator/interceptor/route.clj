(ns commentator.interceptor.route
  (:require [bidi.bidi :as bidi]
            [exoscale.ex :as ex]
            [commentator.handler :as handler]
            [corbihttp.metric :as metric]))

(def admin-calls #{:comment/get :comment/approve :comment/delete-article
                   :comment/delete :comment/admin-for-article :event/list
                   :event/delete})

(def v1
  {[#"/comment/" :article #"/?"] {:post :comment/new}
   [#"/challenge/?"] {:get :challenge/random}
   [#"/comment/" :article #"/?"] {:get :comment/for-article}})

(def admin
  {[#"/comment/" :article #"/?"] {:get :comment/admin-for-article}
   [#"/comment/" :article "/" :comment-id #"/?"] {:get :comment/get}
   [#"/comment/" :article "/" :comment-id #"/?"] {:post :comment/approve}
   [#"/comment/" :article #"/?"] {:delete :comment/delete-article}
   [#"/comment/" :article "/" :comment-id #"/?"] {:delete :comment/delete}
   [#"/event/?"] {:get :event/list}
   [#"/event/" :event-id] {:delete :event/delete}})

(def routes
  ["/"
   [["api/v1" v1]
    ["api/admin" admin]
    [#"healthz/?" :system/healthz]
    [#"health/?" :system/healthz]
    [true :system/not-found]]])

(defn route!
  [request handler registry]
  ;; double :handler because of bidi
  (metric/with-time
    registry
    :http.request.duration
    {"uri" (str (:uri request))
     "method"  (str (some-> request
                            :request-method
                            name))}
    (let [req-handler (:handler request)]
      (condp = req-handler
        :comment/new (handler/new-comment handler request)
        :comment/get (handler/get-comment handler request)
        :comment/for-article (handler/comments-for-article handler request false)
        :comment/approve (handler/approve-comment handler request)
        :comment/delete (handler/delete-comment handler request)
        :comment/delete-article (handler/delete-article-comments handler request)
        :comment/admin-for-article (handler/comments-for-article handler request true)
        :challenge/random (handler/random-challenge handler request)
        :event/list (handler/list-events handler request)
        :event/delete (handler/delete-event handler request)
        :system/healthz (handler/healthz handler request)
        :system/not-found (handler/not-found handler request)
        (throw (ex/ex-fault "unknown handler"
                            {:handler handler}))))))

(defn route
  [handler registry]
  {:name ::route
   :enter
   (fn [{:keys [request] :as ctx}]
     (assoc ctx :response (route! request handler registry)))})

(defn match-route
  [registry]
  {:name ::match-route
   :enter
   (fn [{:keys [request] :as ctx}]
     (let [uri (:uri request)
           request (bidi/match-route* routes uri request)]
       (when (= :system/not-found (:handler request))
         (metric/increment! registry
                            :http.responses.total
                            {"status" "404"
                             "method" (str (some-> (:request ctx)
                                                   :request-method
                                                   name))})
         (throw (ex/ex-info
                 "Not found"
                 [::not-found [:corbi/user ::ex/not-found]])))
       (assoc ctx :request request)))})

