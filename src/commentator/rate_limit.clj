(ns commentator.rate-limit
  "Rate limit for new comments"
  (:require [com.stuartsierra.component :as component]
            [clojure.core.cache.wrapped :as c]
            [exoscale.ex :as ex]))

(defprotocol IRateLimiter
  (validate [this request] "Verifies if this request is not rate limited"))

;; 10 minutes
(def ttl (* 1000 60 10))

(defrecord SimpleRateLimiter [ttl-cache]
  component/Lifecycle
  (start [this]
    (assoc this :ttl-cache (c/ttl-cache-factory {} :ttl ttl)))
  (stop [this]
    (assoc this :ttl-cache nil))
  IRateLimiter
  (validate [this request]
    (let [ip (or (get-in request [:headers "x-forwarded-for"])
                 (:remote-addr request))]
      (if (c/has? ttl-cache ip)
        (throw (ex/ex-forbidden "You are rate limited, please wait"
                                {}))
        (c/miss ttl-cache ip true)))))
