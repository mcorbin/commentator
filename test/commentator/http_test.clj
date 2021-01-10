(ns commentator.http-test
  (:require [clojure.test :refer :all]
            [commentator.http :as http]
            [com.stuartsierra.component :as component]
            [exoscale.cloak :as cloak]))

(deftest auth-admin-test
  (let [token "my-super-token"
        handler (component/start (http/map->ChainHandler
                                  {:token (cloak/mask token)
                                   :api-handler (fn [_]
                                                  {:status 200})}))
        resp-403 {:status 403
                  :body "{\"error\":\"Forbidden\"}",
                  :headers
                  {"content-type" "application/json", "Access-Control-Allow-Origin" "*"}}]
    (is (= resp-403
           (handler {:uri "/api/admin/comment/foo"
                     :request-method :get
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/foo/aaa"
                     :request-method :get
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/foo/aaa"
                     :request-method :post
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/foo"
                     :request-method :delete
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/foo/aaa"
                     :request-method :delete
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/event/"
                     :request-method :get
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/event/foo"
                     :request-method :delete
                     :headers {"authorization" "invalid-token"}})))))
