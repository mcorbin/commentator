(ns commentator.comment-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [commentator.cache :as c]
            [commentator.comment :as comment]
            [commentator.mock.s3 :as ms]
            [com.stuartsierra.component :as component]
            [spy.assert :as assert]
            [spy.protocol :as protocol])
  (:import java.util.UUID))

(def website "mcorbin")

(deftest escape-html-test
  (is (= "foo &amp; &lt;script&gt; &quot;"
         (comment/escape-html "foo & <script> \""))))

(deftest sanitize-test
  (is (= {:content "foo &amp; &lt;script&gt; &quot;"
          :author "&amp; &lt; aa"}
         (comment/sanitize {:content "foo & <script> \""
                            :author "& < aa"}))))

(deftest article-file-name-test
  (is (= "foo.json" (comment/article-file-name "foo"))))

(deftest allowed?-test
  (is (comment/allowed? {website #{"foo"}} website "foo"))
  (is (comment/allowed? nil website "foo"))
  (is (thrown-with-msg?
       Exception
       #"Invalid article bar"
       (comment/allowed? {website #{"foo"}} website "bar")))
  (is (thrown-with-msg?
       Exception
       #"Invalid article bar"
       (comment/allowed? {"lolo" #{"foo"}} website "bar"))))

(defn test-mng
  ([store] (test-mng store false {website #{"foo"}}))
  ([store auto-approve allowed-articles]
   (component/start (comment/map->CommentManager {:auto-approve auto-approve
                                                  :allowed-articles allowed-articles
                                                  :cache (component/start
                                                          (c/map->MemoryCache {}))
                                                  :s3 store}))))

(deftest article-exists-test
  (let [store (ms/store-mock {:exists? (constantly true)})
        mng (test-mng store)]
    (is (comment/article-exists? mng website "foo"))
    (assert/called-with? (:exists? (protocol/spies store))
                         store
                         website
                         "foo.json")))

(defn js
  [data]
  (json/generate-string data))

(deftest for-article-test
  (testing "article exists"
    (let [comments [{:id (UUID/randomUUID)
                     :approved false}
                    {:id (UUID/randomUUID)
                     :approved true}]
          store (ms/store-mock {:get-resource (constantly (js comments))
                                :exists? (constantly true)})
          mng (test-mng store)]
      (testing "filter non approved article"
        (is (= [(second comments)]
               (comment/for-article mng website "foo")))
        (assert/called-with? (:get-resource (protocol/spies store))
                             store
                             website
                             "foo.json")
        (assert/called-n-times? (:get-resource (protocol/spies store)) 1))
      (testing "get all articles"
        (is (= comments
               (comment/for-article mng website "foo" true)))
        ;; hit the cache
        (assert/called-n-times? (:get-resource (protocol/spies store)) 1))

      (is (thrown-with-msg?
           Exception
           #"Invalid article bar"
           (comment/for-article mng website "bar")))))
  (testing "article does not exist"
    (let [store (ms/store-mock {:exists? (constantly false)})
          mng (test-mng store)]
      (is (= [] (comment/for-article mng website "foo" true))))))

(deftest for-article-get-all-test
  (let [comments [{:id (UUID/randomUUID)
                   :approved false}
                  {:id (UUID/randomUUID)
                   :approved true}]
        store (ms/store-mock {:exists? (constantly true)
                              :get-resource (constantly (js comments))})
        mng (test-mng store)]
    (testing "get all articles"
        (is (= comments
               (comment/for-article mng website "foo" true)))
        (assert/called-with? (:get-resource (protocol/spies store))
                             store
                             website
                             "foo.json")
        (assert/called-n-times? (:get-resource (protocol/spies store)) 1))

    (is (thrown-with-msg?
         Exception
         #"Invalid article bar"
         (comment/for-article mng website "bar")))))

(deftest delete-article-test
  (testing "article exists"
    (let [store (ms/store-mock {:exists? (constantly true)
                                :delete-resource (constantly true)})
          mng (test-mng store)]
      (comment/delete-article mng website "foo")
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")
      (assert/called-with? (:delete-resource (protocol/spies store))
                           store
                           website
                           "foo.json")))
  (testing "article does not exist"
    (let [store (ms/store-mock {:exists? (constantly false)
                                :delete-resource (constantly true)})
          mng (test-mng store)]
      (comment/delete-article mng website "foo")
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")
      (assert/not-called? (:delete-resource (protocol/spies store))))))

(deftest add-comment-test
  (testing "not allowed"
    (let [store (ms/store-mock {:exists? (constantly false)})
          mng (test-mng store)]
      (is (thrown-with-msg?
           Exception
           #"Invalid article bar"
           (comment/add-comment mng website "bar" {})))))
  (testing "first comment added"
    (let [store (ms/store-mock {:exists? (constantly false)
                                :save-resource (constantly true)})
          mng (test-mng store)
          comment {:id (UUID/randomUUID)}]
      (comment/add-comment mng website "foo" comment)
      (assert/called-with? (:save-resource (protocol/spies store))
                           store
                           website
                           "foo.json"
                           (json/generate-string [comment]))))
    (testing "non first comment"
      (let [comments [{:id (UUID/randomUUID)}
                      {:id (UUID/randomUUID)}]
            store (ms/store-mock {:exists? (constantly true)
                                  :get-resource (constantly (js comments))
                                  :save-resource (constantly true)})
            mng (test-mng store)
            comment {:id (UUID/randomUUID)
                     :approved true}]
        (comment/add-comment mng website "foo" comment)
        (assert/called-with? (:save-resource (protocol/spies store))
                             store
                             website
                             "foo.json"
                             (json/generate-string (conj comments (assoc comment :approved false))))))
  (testing "auto approve"
      (let [comments [{:id (UUID/randomUUID)}
                      {:id (UUID/randomUUID)}]
            store (ms/store-mock {:exists? (constantly true)
                                  :get-resource (constantly (js comments))
                                  :save-resource (constantly true)})
            mng (test-mng store true {website #{"foo"}})
            comment {:id (UUID/randomUUID)
                     :approved false}]
        (comment/add-comment mng website "foo" comment)
        (assert/called-with? (:save-resource (protocol/spies store))
                             store
                             website
                             "foo.json"
                             (json/generate-string (conj comments (assoc comment :approved true)))))))

(deftest approve-comment-test
  (testing "article does not exist"
    (let [store (ms/store-mock {:exists? (constantly false)})
          mng (test-mng store)]
      (is (thrown-with-msg?
           Exception
           #"No comment for article foo"
           (comment/approve-comment mng website "foo" (UUID/randomUUID))))
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")))
  (testing "article exists, comment does not exist"
    (let [id (UUID/randomUUID)
          store (ms/store-mock {:exists? (constantly true)
                                :get-resource (constantly (js []))})
          mng (test-mng store)]
      (is (thrown-with-msg?
           Exception
           #"not found for article foo"
           (comment/approve-comment mng website "foo" id)))
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")))
  (testing "article and comment exists"
    (let [id (UUID/randomUUID)
          store (ms/store-mock {:exists? (constantly true)
                                :save-resource (constantly true)
                                :get-resource (constantly (js [{:id id
                                                                :approved false}]))})
          mng (test-mng store)]
      (comment/approve-comment mng website "foo" id)
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")
      (assert/called-with? (:save-resource (protocol/spies store))
                           store
                           website
                           "foo.json"
                           (json/generate-string [{:id id :approved true}]))))
  (testing "article and comment exists, multiple article"
    (let [id (UUID/randomUUID)
          events [{:id id
                   :approved false}
                  {:id (UUID/randomUUID)
                   :approved false}]
          store (ms/store-mock {:exists? (constantly true)
                                :save-resource (constantly true)
                                :get-resource (constantly (js events))})
          mng (test-mng store)]
      (comment/approve-comment mng website "foo" id)
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")
      (assert/called-with? (:save-resource (protocol/spies store))
                           store
                           website
                           "foo.json"
                           (json/generate-string (assoc-in events [0 :approved] true))))))


(deftest delete-comment-test
  (testing "article and comment exists"
    (let [id (UUID/randomUUID)
          events [{:id id
                   :approved false}
                  {:id (UUID/randomUUID)
                   :approved false}]
          store (ms/store-mock {:exists? (constantly true)
                                :get-resource (constantly (js events))
                                :save-resource (constantly true)})
          mng (test-mng store)]
      (comment/delete-comment mng website "foo" id)
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")
      (assert/called-with? (:save-resource (protocol/spies store))
                           store
                           website
                           "foo.json"
                           (json/generate-string [(last events)]))))
  (testing "article exists, comment does not exist"
    (let [id (UUID/randomUUID)
          events [{:id (UUID/randomUUID)
                   :approved false}
                  {:id (UUID/randomUUID)
                   :approved false}]
          store (ms/store-mock {:exists? (constantly true)
                                :get-resource (constantly (js events))
                                :save-resource (constantly true)})
          mng (test-mng store)]
      (is (thrown-with-msg?
           Exception
           #"not found for article foo"
           (comment/delete-comment mng website "foo" id)))
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")
      (assert/not-called? (:save-resource (protocol/spies store)))))
  (testing "article does not exist"
    (let [id (UUID/randomUUID)
          store (ms/store-mock {:exists? (constantly false)})
          mng (test-mng store)]
      (is (thrown-with-msg?
           Exception
           #"No comment for article foo"
           (comment/delete-comment mng website "foo" id))))))

(deftest get-comment-test
  (testing "article and comment exists"
    (let [id (UUID/randomUUID)
          events [{:id id
                   :approved false}
                  {:id (UUID/randomUUID)
                   :approved false}]
          store (ms/store-mock {:exists? (constantly true)
                                :get-resource (constantly (js events))})
          mng (test-mng store)]
      (is (= {:id id
              :approved false}
             (comment/get-comment mng website "foo" id)))
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")))
  (testing "article exists, comment does not exist"
    (let [id (UUID/randomUUID)
          events [{:id (UUID/randomUUID)
                   :approved false}
                  {:id (UUID/randomUUID)
                   :approved false}]
          store (ms/store-mock {:exists? (constantly true)
                                :get-resource (constantly (js events))})
          mng (test-mng store)]
      (is (thrown-with-msg?
           Exception
           #"not found for article foo"
           (comment/get-comment mng website "foo" id)))
      (assert/called-with? (:exists? (protocol/spies store))
                           store
                           website
                           "foo.json")))
  (testing "article does not exist"
    (let [id (UUID/randomUUID)
          store (ms/store-mock {:exists? (constantly false)})
          mng (test-mng store)]
      (is (thrown-with-msg?
           Exception
           #"No comment for article foo"
           (comment/get-comment mng website "foo" id))))))
