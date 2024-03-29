(ns commentator.rate-limit
  "Rate limit for new comments"
  (:require [com.stuartsierra.component :as component]
            [clojure.core.cache.wrapped :as c]
            [exoscale.ex :as ex]))

(defprotocol IRateLimiter
  (validate [this request website] "Verifies if this request is not rate limited"))

(defn source-ip
  [request]
  (or (get-in request [:headers "x-forwarded-for"])
      (:remote-addr request)))

(defrecord SimpleRateLimiter [rate-limit-minutes ttl-cache]
  component/Lifecycle
  (start [this]
    (assoc this :ttl-cache (c/ttl-cache-factory {} :ttl (* 1000 60 rate-limit-minutes))))
  (stop [this]
    (assoc this :ttl-cache nil))
  IRateLimiter
  (validate [_ request website]
    (let [ip (source-ip request)
          cache-key (str website "-" ip)]
      (if (c/has? ttl-cache cache-key)
        (throw (ex/ex-info "You are rate limited, please wait"
                           [::rate-limited [:corbi/user ::ex/forbidden]]
                           {}))
        (do (c/miss ttl-cache cache-key true)
            true)))))
