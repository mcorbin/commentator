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

(def website "mcorbin")

(deftest req->article-test
  (is (= "foo" (h/req->article {:all-params {:article "foo"}}))))

(deftest req->comment-id-test
  (let [id (UUID/randomUUID)]
    (is (= id (h/req->comment-id {:all-params {:comment-id id}})))))

(deftest req->event-id-test
  (let [id (UUID/randomUUID)]
    (is (= id (h/req->event-id {:all-params {:event-id id}})))))

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
    (let [response (h/new-comment handler {:all-params {:content "content"
                                                        :website website
                                                        :author "mcorbin"
                                                        :challenge :c1
                                                        :answer "bar"
                                                        :article "foo"}})]
      (is (= {:status 201
              :body {:message "Comment added"}}
             response)))
    (Thread/sleep 100)
    (let [calls (spy/calls (:save-resource (protocol/spies store)))
          [p1 p2 p3 p4] (first calls)
          [c1 :as comments] (json/parse-string p4 true)
          [pe1 pe2 pe3 pe4] (second calls)
          [e1 :as events] (json/parse-string pe4 true)]
      ;; 1 event 1 comment
      (is (= 2 (count calls)))
      (is (= store p1))
      (is (= store pe1))
      (is (= website p2))
      (is (= website pe2))
      (is (= "foo.json" p3))
      (is (= "events.json" pe3))
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
           (h/get-comment handler {:all-params {:article "foo"
                                                :website website
                                                :comment-id id}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         website
                         "foo.json")
    (assert/called-with? (:get-resource (protocol/spies store))
                         store
                         website
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
                                   {:all-params {:article "foo"
                                                 :website website}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         website
                         "foo.json")
    (assert/called-with? (:get-resource (protocol/spies store))
                         store
                         website
                         "foo.json")))

(deftest admin-for-article-test
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
           (h/admin-for-article handler
                                {:all-params {:article "foo"
                                              :website website}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         website
                         "foo.json")
    (assert/called-with? (:get-resource (protocol/spies store))
                         store
                         website
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
           (h/delete-comment handler {:all-params {:article "foo"
                                                   :website website
                                                   :comment-id id}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         website
                         "foo.json")
    (assert/called-with? (:save-resource (protocol/spies store))
                         store
                         website
                         "foo.json"
                         (json/generate-string [(last events)]))))

(deftest delete-article-comments-test
  (let [store (ms/store-mock {:exists? (constantly true)
                              :delete-resource (constantly true)})
        comment-mng (ct/test-mng store)
        handler (h/map->Handler {:comment-manager comment-mng})]
    (is (= {:status 200 :body {:message "Comments deleted"}}
           (h/delete-article-comments handler {:all-params {:article "foo"
                                                            :website website}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         website
                         "foo.json")
    (assert/called-with? (:delete-resource (protocol/spies store))
                         store
                         website
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
           (h/approve-comment handler {:all-params {:article "foo"
                                                    :website website
                                                    :comment-id id}})))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         website
                         "foo.json")
    (assert/called-with? (:save-resource (protocol/spies store))
                         store
                         website
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
