(ns commentator.auth-test
  (:require [clojure.test :refer :all]
            [commentator.auth :as auth]
            [exoscale.cloak :as cloak]))

(deftest auth-test
  (let [token (cloak/mask "my-super-token")]
    (is (map? (auth/auth-handler
               {:handler :comment/delete
                :request {:headers {"authorization" "my-super-token"}}}
               token)))
    (is (map? (auth/auth-handler
               {:handler :challenge/random
                :request {:headers {"authorization" "my-super-token"}}}
               token)))
    (is (map? (auth/auth-handler
               {:handler :challenge/random}
               token)))
    (is (thrown-with-msg?
         Exception
         #"Forbidden"
         (auth/auth-handler
          {:handler :comment/delete
           :request {:headers {"authorization" "invalid token"}}}
          token)))
    (is (thrown-with-msg?
         Exception
         #"Forbidden"
         (auth/auth-handler
          {:handler :comment/delete}
          token)))))
