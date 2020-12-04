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
    (let [response (h/new-comment handler {:body {:content "content"
                                                  :author "mcorbin"
                                                  :challenge :c1
                                                  :answer "bar"}
                                           :route-params {:article "foo"}})]
         (is (= {:status 201
                 :body {:message "Comment added"}}
                response)))
    (Thread/sleep 100)
    (let [calls (spy/calls (:save-resource (protocol/spies store)))
          [p1 p2 p3] (first calls)
          [c1 :as comments] (json/parse-string p3 true)
          [pe1 pe2 pe3] (second calls)
          [e1 :as events] (json/parse-string pe3 true)]
      ;; 1 event 1 comment
      (is (= 2 (count calls)))
      (is (= store p1))
      (is (= store pe1))
      (is (= "foo.json" p2))
      (is (= "events.json" pe2))
      (is (= 1 (count comments)))
      (is (= 1 (count events)))
      (is (= {:content "content"
              :author "mcorbin"
              :approved false}
             (select-keys c1 [:content :author :approved])))
      (is (uuid? (UUID/fromString (:id c1))))
      (is (pos-int? (:timestamp c1)))
      (is (= {:article "foo"
              :type "new-comment"}
             (select-keys e1 [:article :type]))))))

(deftest get-comment-test
  (let [id (UUID/randomUUID)
        comments [{:id id
                   :approved false}
                  {:id (UUID/randomUUID)
                   :approved false}]
        store (ms/store-mock {:exists? (constantly true)
                              :get-resource (constantly (ct/js comments))})
        comment-mng (ct/test-mng store)
        handler (h/map->Handler {:comment-manager comment-mng})]
    (is (= {:status 200
            :body {:id id
                   :approved false}}
           (h/get-comment handler {:route-params {:article "foo"
                                                  :comment-id id}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         "foo.json")
    (assert/called-with? (:get-resource (protocol/spies store))
                         store
                         "foo.json")))

(deftest comments-for-article-test
  (let [id1 (UUID/randomUUID)
        id2 (UUID/randomUUID)
        comments [{:id id1
                   :approved true}
                  {:id id2
                   :approved false}]
        store (ms/store-mock {:exists? (constantly true)
                              :get-resource (constantly (ct/js comments))})
        comment-mng (ct/test-mng store)
        handler (h/map->Handler {:comment-manager comment-mng})]
    (is (= {:status 200
            :body [{:id id1
                     :approved true}]}
           (h/comments-for-article handler
                                   {:route-params {:article "foo"}}
                                   false)))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         "foo.json")
    (assert/called-with? (:get-resource (protocol/spies store))
                         store
                         "foo.json")))

(deftest comments-for-article-all-test
  (let [id1 (UUID/randomUUID)
        id2 (UUID/randomUUID)
        comments [{:id id1
                   :approved true}
                  {:id id2
                   :approved false}]
        store (ms/store-mock {:exists? (constantly true)
                              :get-resource (constantly (ct/js comments))})
        comment-mng (ct/test-mng store)
        handler (h/map->Handler {:comment-manager comment-mng})]
    (is (= {:status 200
            :body comments}
           (h/comments-for-article handler
                                   {:route-params {:article "foo"}}
                                   true)))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         "foo.json")
    (assert/called-with? (:get-resource (protocol/spies store))
                         store
                         "foo.json")))

(deftest delete-comment-test
  (let [id (UUID/randomUUID)
        events [{:id id
                 :approved false}
                {:id (UUID/randomUUID)
                 :approved false}]
        store (ms/store-mock {:exists? (constantly true)
                              :get-resource (constantly (ct/js events))
                              :save-resource (constantly true)})
        comment-mng (ct/test-mng store)
        handler (h/map->Handler {:comment-manager comment-mng})]
    (is (= {:status 200 :body {:message "Comment deleted"}}
         (h/delete-comment handler {:route-params {:article "foo"
                                                     :comment-id id}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         "foo.json")
    (assert/called-with? (:save-resource (protocol/spies store))
                         store
                         "foo.json"
                         (json/generate-string [(last events)]))))

(deftest delete-article-comments-test
  (let [store (ms/store-mock {:exists? (constantly true)
                              :delete-resource (constantly true)})
        comment-mng (ct/test-mng store)
        handler (h/map->Handler {:comment-manager comment-mng})]
    (is (= {:status 200 :body {:message "Comments deleted"}}
            (h/delete-article-comments handler {:route-params {:article "foo"}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         "foo.json")
    (assert/called-with? (:delete-resource (protocol/spies store))
                         store
                         "foo.json")))

(deftest approve-comment-test
  (let [id (UUID/randomUUID)
        store (ms/store-mock {:exists? (constantly true)
                              :save-resource (constantly true)
                              :get-resource (constantly (ct/js [{:id id
                                                                 :approved false}]))})
        comment-mng (ct/test-mng store)
        handler (h/map->Handler {:comment-manager comment-mng})]
    (is (= {:status 200 :body {:message "Comment approved"}}
           (h/approve-comment handler {:route-params {:article "foo"
                                                      :comment-id id}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         "foo.json")
    (assert/called-with? (:save-resource (protocol/spies store))
                         store
                         "foo.json"
                         (json/generate-string [{:id id :approved true}]))))

(deftest healthz-test
  (let [handler (h/map->Handler {})]
    (is (= {:status 200
            :body {:message "ok"}}
           (h/healthz handler {})))))

(deftest not-found-test
  (let [handler (h/map->Handler {})]
    (is (= {:status 404
            :body {:error "not found"}}
           (h/not-found handler {})))))
