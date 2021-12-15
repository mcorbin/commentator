(ns commentator.lock
  (:require [com.stuartsierra.component :as component]))

(defprotocol ILock
  (get-lock [this lock-name] "Returns a lock by name. Creates one if it does not exist"))

(defrecord Lock [lock-map]
  component/Lifecycle
  (start [this]
    (assoc this :lock-map (atom {})))
  (stop [this]
    (assoc this :lock-map nil))
  ILock
  (get-lock [_ lock-name]
    (get (swap! lock-map (fn [state]
                           (if (get state lock-name)
                             state
                             (assoc state lock-name (java.lang.Object.)))))
         lock-name)))
