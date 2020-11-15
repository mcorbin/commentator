(ns commentator.handler-test
  (:require [clojure.test :refer :all]
            [commentator.comment-test :as ct]
            [commentator.handler :as h]
            [commentator.mock.s3 :as ms]))

(deftest req->article-test
  (is (= "foo" (h/req->article {:route-params {:article "foo"}}))))
