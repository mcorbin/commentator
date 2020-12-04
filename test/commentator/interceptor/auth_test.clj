(ns commentator.interceptor.auth-test
  (:require [clojure.test :refer :all]
            [commentator.interceptor.auth :as auth]
            [exoscale.cloak :as cloak]))

(deftest auth-test
  (let [itc (auth/auth (cloak/mask "my-super-token"))]
    (is (map? ((:enter itc)
               {:request {:handler :comment/delete
                          :headers {"authorization" "my-super-token"}}})))
    (is (map? ((:enter itc)
               {:request {:handler :challenge/random
                          :headers {"authorization" "my-super-token"}}})))
    (is (map? ((:enter itc)
               {:request {:handler :challenge/random}})))
    (is (thrown-with-msg?
         Exception
         #"Forbidden"
         ((:enter itc)
          {:request {:handler :comment/delete
                     :headers {"authorization" "invalid token"}}})))
    (is (thrown-with-msg?
         Exception
         #"Forbidden"
         ((:enter itc)
          {:request {:handler :comment/delete}})))))
