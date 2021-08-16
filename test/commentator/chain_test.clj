(ns commentator.chain-test
  (:require [clojure.test :refer :all]
            [commentator.handler :as h]
            [commentator.chain :as chain]
            [exoscale.cloak :as cloak]
            [exoscale.interceptor :as interceptor]))

(deftest auth-admin-test
  (let [token "my-super-token"
        chain (chain/interceptor-chain {:token (cloak/mask token)
                                        :allow-origin #{"foo.com"}
                                        :api-handler
                                        (reify h/IHandler
                                          (new-comment [this request] {:status 200})
                                          (get-comment [this request] {:status 200})
                                          (comments-for-article [this request] {:status 200})
                                          (admin-for-article [this request] {:status 200})
                                          (delete-comment [this request] {:status 200})
                                          (delete-article-comments [this request] {:status 200})
                                          (approve-comment [this request] {:status 200})
                                          (random-challenge [this request] {:status 200})
                                          (list-events [this request] {:status 200})
                                          (delete-event [this request] {:status 200})
                                          (healthz [this request] {:status 200})
                                          (metrics [this request] {:status 200})
                                          (not-found [this request] {:status 404}))})
        handler (fn [request] (interceptor/execute {:request request} chain))
        resp-403 {:status 403
                  :body "{\"error\":\"Forbidden\"}",
                  :headers
                  {"content-type" "application/json"
                   "Access-Control-Allow-Origin" "foo.com"
                   "Vary" "Origin"}}]
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo"
                     :request-method :get
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo/aaa"
                     :request-method :get
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo/aaa"
                     :request-method :post
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo"
                     :request-method :delete
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo/aaa"
                     :request-method :delete
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/event/mcorbin"
                     :request-method :get
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/event/mcorbin/foo"
                     :request-method :delete
                     :headers {"authorization" "invalid-token"}})))))
