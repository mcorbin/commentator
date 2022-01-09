(ns commentator.auth-test
  (:require [clojure.test :refer :all]
            [commentator.auth :as auth]
            [exoscale.cloak :as cloak]))

(deftest auth-test
  (let [token (cloak/mask "my-super-token")]
    (is (map? (auth/auth-handler
               {:handler {:auth true}
                :request {:headers {"authorization" "my-super-token"}}}
               token)))
    (is (map? (auth/auth-handler
               {:handler {:auth false}
                :request {:headers {"authorization" "my-super-token"}}}
               token)))
    (is (map? (auth/auth-handler
               {:handler {:auth false}}
               token)))
    (is (thrown-with-msg?
         Exception
         #"Forbidden"
         (auth/auth-handler
          {:handler {:auth true}
           :request {:headers {"authorization" "invalid token"}}}
          token)))
    (is (thrown-with-msg?
         Exception
         #"Forbidden"
         (auth/auth-handler
          {:handler {:auth true}}
          token)))))
