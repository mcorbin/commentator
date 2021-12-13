(ns commentator.cache
  (:require [clojure.core.cache.wrapped :as c]
            [com.stuartsierra.component :as component]))

(defprotocol Cache
  (lookup [this website article])
  (miss [this website article comments])
  (evict [this website article]))

(def cache-ttl (* 1000 60 60 24))

(defn cache-key
  [website article]
  (str website "/" article))

(defrecord MemoryCache [cache]
  component/Lifecycle
  (start [this]
    (assoc this :cache (c/ttl-cache-factory {} :ttl cache-ttl)))
  (stop [this]
    (assoc this :cache nil))
  Cache
  (lookup [_ website article]
    (c/lookup cache (cache-key website article)))
  (miss [_ website article comments]
    (c/miss cache (cache-key website article) comments))
  (evict [_ website article]
    (c/evict cache (cache-key website article))))
