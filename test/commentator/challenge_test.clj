(ns commentator.challenge-test
  (:require [clojure.test :refer :all]
            [commentator.challenge :as c]))

(deftest verify-test
  (let [challenges {:c1 {:question "1 + 4 = ?"
                         :answer "5"}
                    :c2 {:question "???"
                         :answer "chaussette"}}]
    (is (c/verify challenges :c1 "5"))
    (is (c/verify challenges :c2 "chaussette"))
    (is (c/verify challenges :c2 "CHAUSSETTE"))
    (is (thrown-with-msg?
         Exception
         #"Bad challenge response"
         (c/verify challenges :c1 "6")))))
