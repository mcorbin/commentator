(ns commentator.handler-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [commentator.comment-test :as ct]
            [commentator.event :as event]
            [commentator.handler :as h]
            [commentator.mock.s3 :as ms]
            [commentator.rate-limit :as rl]
            [spy.assert :as assert]
            [spy.core :as spy]
            [spy.protocol :as protocol])
  (:import java.util.UUID))

(deftest req->article-test
  (is (= "foo" (h/req->article {:route-params {:article "foo"}}))))

(deftest req->comment-id-test
  (let [id (UUID/randomUUID)]
    (is (= id (h/req->comment-id {:route-params {:comment-id id}})))
    (is (= id (h/req->comment-id {:route-params {:comment-id (str id)}}))))
  (is (thrown-with-msg?
       Exception
       #"Invalid spec"
       (h/req->comment-id {:route-params {:comment-id "a"}}))))

(deftest req->event-id-test
  (let [id (UUID/randomUUID)]
    (is (= id (h/req->event-id {:route-params {:event-id id}})))
    (is (= id (h/req->event-id {:route-params {:event-id (str id)}}))))
  (is (thrown-with-msg?
       Exception
       #"Invalid spec"
       (h/req->event-id {:route-params {:event-id "a"}}))))

(deftest new-comment-test
  (let [store (ms/store-mock {:exists? (constantly true)
                              :get-resource (constantly "[]")
                              :save-resource (constantly nil)})
        comment-mng (ct/test-mng store)
        event-mng (event/map->EventManager {:s3 store
                                        :lock (Object.)})
        handler (h/map->Handler {:comment-manager comment-mng
                                 :event-manager event-mng
                                 :challenges {:c1 {:question "foo" :answer "bar"}}
                                 :rate-limiter (component/start (rl/map->SimpleRateLimiter {}))})]
    (h/new-comment handler {:body {:content "content"
                                   :author "mcorbin"
                                   :challenge :c1
                                   :answer "bar"}
                            :route-params {:article "foo"}})
    (let [calls (spy/calls (:save-resource (protocol/spies store)))
          [p1 p2 p3] (first calls)
          [c1 :as comments] (json/parse-string p3 true)]
      (is (= 1 (count calls)))
      (is (= store p1))
      (is (= "foo.json" p2))
      (is (= 1 (count comments)))
      (is (= {:content "content"
              :author "mcorbin"
              :approved false}
             (select-keys c1 [:content :author :approved])))
      (is (uuid? (UUID/fromString (:id c1))))
      (is (pos-int? (:timestamp c1))))))
