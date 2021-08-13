(ns commentator.interceptor.cors-test
  (:require [clojure.test :refer :all]
            [commentator.interceptor.cors :as cors]))

(deftest get-origin-test
  (is (= "foo.com"
         (cors/get-origin {:headers {"Origin" "foo.com"}}
                          #{"bar.com" "foo.com"})))
    (is (= "bar.com"
         (cors/get-origin {:headers {"Origin" "foo.com"}}
                          #{"bar.com"}))))
