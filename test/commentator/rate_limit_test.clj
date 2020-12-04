(ns commentator.rate-limit-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]
            [commentator.rate-limit :as rl]))

(deftest rate-limiter-test
  (let [limiter (component/start (rl/map->SimpleRateLimiter {}))]
    (is (rl/validate limiter {:remote-addr "10.1.1.2"}))
    (is (thrown-with-msg?
         Exception
         #"You are rate limited, please wait"
         (rl/validate limiter {:remote-addr "10.1.1.2"})))
    (is (rl/validate limiter {:remote-addr "10.1.1.2"
                              :headers {"x-forwarded-for" "10.3.3.2"}}))
    (is (thrown-with-msg?
         Exception
         #"You are rate limited, please wait"
         (rl/validate limiter {:remote-addr "10.1.1.2"
                               :headers {"x-forwarded-for" "10.3.3.2"}})))
    (is (thrown-with-msg?
         Exception
         #"You are rate limited, please wait"
         (rl/validate limiter {:headers {"x-forwarded-for" "10.3.3.2"}})))))
