(ns commentator.challenge-test
  (:require [clojure.test :refer :all]
            [commentator.challenge :as c])
  (:import commentator.challenge.ChallengeManager))

(deftest questions-challenges-test
  (let [challenges {:type :questions
                    :ttl 10
                    :secret "azeaeazazeea"
                    :questions [{:question "1 + 4 = ?"
                                 :answer "5"}]}
        mng (ChallengeManager. challenges)
        challenge (c/random-challenge mng "mcorbin-fr" "test")]
    (is (= "1 + 4 = ?" (:question challenge)))
    (is (string? (:signature challenge)))
    (is (pos-int? (:timestamp challenge)))
    (is (c/verify mng (assoc challenge :answer "5" :website "mcorbin-fr" :article "test")))
    (is (thrown-with-msg?
         Exception
         #"Bad challenge response"
         (c/verify mng (assoc challenge :answer "10" :website "mcorbin-fr" :article "test"))))
    (is (thrown-with-msg?
         Exception
         #"Bad challenge response"
         (c/verify mng (assoc challenge :answer "5" :website "mcorbin-f" :article "test"))))
    (is (thrown-with-msg?
         Exception
         #"Bad challenge response"
         (c/verify mng (assoc challenge :answer "5" :website "mcorbin-fr" :article "tes"))))
    (is (thrown-with-msg?
         Exception
         #"The challenge has expired"
         (c/verify mng (-> (assoc challenge :answer "5" :website "mcorbin-fr" :article "test")
                           (update :timestamp - 20000)))))))

(deftest math-challenges-test
  (let [challenges {:type :math
                    :ttl 10
                    :secret "azeaeazazeea"}
        mng (ChallengeManager. challenges)
        challenge (c/random-challenge mng "mcorbin-fr" "test")]
    (is (string? (:question challenge)))
    (is (.contains ^String (:question challenge) "what is the result of"))
    (is (string? (:signature challenge)))
    (is (pos-int? (:timestamp challenge)))))
