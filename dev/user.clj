(ns user
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [commentator.core :as core]))

(defn start!
  []
  (refresh)
  (core/start!)
  "started")

(defn stop!
  []
  (core/stop!)
  "stopped")

(defn restart!
  []
  (stop!)
  (start!))
