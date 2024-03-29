(ns commentator.event-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [commentator.event :as event]
            [commentator.mock.s3 :as ms]
            [commentator.lock :as lock]
            [com.stuartsierra.component :as component]
            [spy.assert :as assert]
            [spy.protocol :as protocol])
  (:import java.util.UUID))

(defn js
  [data]
  (json/generate-string data))

(def website "mcorbin")

(deftest list-events-test
  (testing "some events exist"
    (let [events [{:id (UUID/randomUUID)
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}
                  {:id (UUID/randomUUID)
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}]
          store (ms/store-mock {:get-resource (constantly (js events))
                                :exists? (constantly true)})
          mng (event/map->EventManager {:s3 store
                                        :lock (component/start (lock/map->Lock {}))})]
      (is (= events (event/list-events mng website)))))
  (testing "No events"
    (let [store (ms/store-mock {:exists? (constantly false)})
          mng (event/map->EventManager {:s3 store})]
      (is (= [] (event/list-events mng website))))))

(deftest add-event-test
  (testing "some events exist"
    (let [events [{:id (UUID/randomUUID)
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}
                  {:id (UUID/randomUUID)
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}]
          event {:id (UUID/randomUUID)
                 :timestamp (System/currentTimeMillis)
                 :type :new-comment}
          store (ms/store-mock {:get-resource (constantly (js events))
                                :save-resource (constantly true)
                                :exists? (constantly true)})
          mng (event/map->EventManager {:s3 store
                                        :lock (component/start (lock/map->Lock {}))})]
      (event/add-event mng website event)
      (assert/called-with? (:save-resource (protocol/spies store))
                           store
                           website
                           event/event-file-name
                           (json/generate-string (conj events event)))))
  (testing "No events"
    (let [store (ms/store-mock {:exists? (constantly false)
                                :save-resource (constantly true)})
          event {:id (UUID/randomUUID)
                 :timestamp (System/currentTimeMillis)
                 :type :new-comment}
          mng (event/map->EventManager {:s3 store
                                        :lock (component/start (lock/map->Lock {}))})]
      (event/add-event mng website event)
      (assert/called-with? (:save-resource (protocol/spies store))
                           store
                           website
                           event/event-file-name
                           (json/generate-string [event])))))

(deftest delete-events-test
  (testing "The event exists"
    (let [id (UUID/randomUUID)
          events [{:id id
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}
                  {:id (UUID/randomUUID)
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}]
          store (ms/store-mock {:get-resource (constantly (js events))
                                :save-resource (constantly true)
                                :exists? (constantly true)})
          mng (event/map->EventManager {:s3 store
                                        :lock (component/start (lock/map->Lock {}))})]
      (event/delete-event mng website id)
      (assert/called-with? (:save-resource (protocol/spies store))
                           store
                           website
                           event/event-file-name
                           (json/generate-string [(second events)]))))
  (testing "No event"
    (let [id (UUID/randomUUID)
          store (ms/store-mock {:exists? (constantly false)})
          mng (event/map->EventManager {:s3 store
                                        :lock (component/start (lock/map->Lock {}))})]
      (is (thrown-with-msg?
           Exception
           #"not found"
           (event/delete-event mng website id)))))
  (testing "Even does not exist"
    (let [id (UUID/randomUUID)
          events [{:id (UUID/randomUUID)
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}
                  {:id (UUID/randomUUID)
                   :timestamp (System/currentTimeMillis)
                   :type :new-comment}]
          store (ms/store-mock {:get-resource (constantly (js events))
                                :exists? (constantly false)})
          mng (event/map->EventManager {:s3 store
                                        :lock (component/start (lock/map->Lock {}))})]
      (is (thrown-with-msg?
           Exception
           #"not found"
           (event/delete-event mng website id))))))
