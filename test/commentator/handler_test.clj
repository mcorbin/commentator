(ns commentator.handler-test
  (:require [clojure.test :refer :all]
            [commentator.comment-test :as ct]
            [commentator.handler :as h]
            [commentator.mock.s3 :as ms])
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
