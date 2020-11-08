(ns commentator.challenge
  (:require [exoscale.ex :as ex]))

(defn verify
  [challenges challenge-name answer]
  (when-not (= answer
               (get-in challenges [challenge-name :answer]))
    (throw (ex/ex-incorrect "Bad challenge response" {})))
  true)
