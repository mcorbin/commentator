(ns commentator.rate-limit
  "Rate limit for new comments"
  (:require [com.stuartsierra.component :as component]
            [clojure.core.cache.wrapped :as c]
            [exoscale.ex :as ex]))

(defprotocol IRateLimiter
  (validate [this request] "Verifies if this request is not rate limited"))

(defrecord SimpleRateLimiter [rate-limit-minutes ttl-cache]
  component/Lifecycle
  (start [this]
    (assoc this :ttl-cache (c/ttl-cache-factory {} :ttl (* 1000 60 rate-limit-minutes))))
  (stop [this]
    (assoc this :ttl-cache nil))
  IRateLimiter
  (validate [this request]
    (let [ip (or (get-in request [:headers "x-forwarded-for"])
                 (:remote-addr request))]
      (if (c/has? ttl-cache ip)
        (throw (ex/ex-forbidden "You are rate limited, please wait"
                                {}))
        (do (c/miss ttl-cache ip true)
            true)))))
