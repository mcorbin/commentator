(ns commentator.chain-test
  (:require [clojure.test :refer :all]
            [commentator.handler :as h]
            [commentator.chain :as chain]
            [corbihttp.b64 :as b64]
            [exoscale.cloak :as cloak]
            [exoscale.interceptor :as interceptor]))

(deftest auth-admin-test
  (let [username "toto"
        password "abc123"
        auth-header (str "Basic " (b64/to-base64 (str username ":" password)))
        chain (chain/interceptor-chain {:username username
                                        :password (cloak/mask password)
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
        resp-403 {:status 401
                  :headers {"WWW-Authenticate" "Basic realm=\"commentator\""}
                  :exoscale.interceptor/queue nil
                  :exoscale.interceptor/stack nil}]
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo"
                     :request-method :get
                     :headers {"authorization" "invalid-token"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo/aaa"
                     :request-method :get
                     :headers {"authorization" "foo:bar"}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo/aaa"
                     :request-method :post
                     :headers {"authorization" (str "Basic " (b64/to-base64 (str username ":AA")))}})))
    (is (= resp-403
           (handler {:uri "/api/admin/comment/mcorbin/foo"
                     :request-method :delete
                     :headers {"authorization" (str "Basic " (b64/to-base64 (str "abc:" password)))}})))
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
