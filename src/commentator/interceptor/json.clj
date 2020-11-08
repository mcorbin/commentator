(ns commentator.interceptor.json
  (:require [byte-streams :as bs]
            [cheshire.core :as json]
            [exoscale.ex :as ex]))

(def json
  {:name ::json
   :leave (fn [ctx]
            (if (coll? (get-in ctx [:response :body]))
              (-> (update-in ctx [:response :body] json/generate-string)
                  (update-in [:response :headers] assoc "content-type"
                             "application/json"))
              ctx))})

(def request-params
  {:name ::json-params
   :enter (fn [ctx]
            (let [request (:request ctx)]
              (if (:body request)
                (try
                  (update-in ctx
                             [:request :body]
                             (fn [body]
                               (-> (bs/convert body String)
                                   (json/parse-string true))))
                  (catch Exception _
                    (throw (ex/ex-incorrect "fail to convert the request body to json"))))
                ctx)))})
