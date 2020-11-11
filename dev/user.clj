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

(defn challenge
  []
  (let [operations [* + -]
        mapping {* " * " + " + " - " - "}
        n1 (rand-int 12)
        n2 (rand-int 12)
        op (rand-nth operations)
        result (op n1 n2)
        ]
    ()
    {:question (format "what is the result of: %d %s %d"
                       n1
                       (get mapping op)
                       n2)
     :answer (str result)}))

(defn challenges
  [n]
  (->> {:challenges (reduce (fn [state _] (assoc state
                                                 (keyword (str (java.util.UUID/randomUUID)))
                                                 (challenge)))
                            {}
                            (range n))}
      pr-str
      (spit "/tmp/challenges.edn"))
  
  )
