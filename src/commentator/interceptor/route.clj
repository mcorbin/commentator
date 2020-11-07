(ns commentator.interceptor.route
  (:require [bidi.bidi :refer [match-route*]]
            [exoscale.ex :as ex]
            [commentator.log :as log]))

(def v1
  {[#"/comment/?" :article] {:post {:handler :comment/new}}
   [#"/comment/?" :article] {:get {:handler :comment/for-article}}})

(def admin
  {[#"/comment/?" :article "/" :comment-id] {:get {:handler :comment/get
                                                   :admin? true}}
   [#"/comment/?" :article "/" :comment-id] {:post {:handler :comment/approve
                                                    :admin? true}}
   [#"/comment/?" :article "/?"] {:delete {:handler :comment/delete-article
                                           :admin? true}}
   [#"/comment/?" :article "/" :comment-id] {:delete {:handler :comment/delete
                                                      :admin? true}}
   [#"/event/?"] {:get {:handler :event/list
                        :admin? true}}})

(def routes
  ["/"
   [["api/v1" v1]
    ["api/admin" admin]
    [#"healthz/?" :system/healthz]
    [#"health/?" :system/healthz]
    [#"metrics/?" :system/metrics]
    [true :system/not-found]]])

(defn route!
  [request]
  ;; double :handler because of bidi
  (let [handler (-> (get-in request [:handler :handler]))]
    (condp = handler
      :comment/new ""
      :comment/get ""
      :comment/for-article ""
      :comment/approve ""
      :comment/delete ""
      :event/list ""
      :system/healthz ""
      :system/metrics ""
      :system/not-found ""
      (throw (ex/ex-fault "unknown handler"
                          {:handler handler}))
      )
    )
  )

(def route
  {:name ::route
   :enter
   (fn [{:keys [request] :as ctx}]
     (log/debug {:uri (:uri request) :method (:request-method request)}
                "http request")
     (assoc ctx :response (route! request)))})

(def match-route
  {:name ::match-route
   :enter
   (fn [{:keys [request] :as ctx}]
     (let [uri (:uri request)]
       (assoc ctx :request (match-route* routes uri request))))})

