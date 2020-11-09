(ns commentator.challenge
  (:require [clojure.string :as string]
            [exoscale.ex :as ex]))

(defn verify
  [challenges challenge-name answer]
  (when-not (and (get-in challenges [challenge-name :answer])
             (= (string/lower-case answer)
                (string/lower-case (get-in challenges [challenge-name :answer]))))
    (throw (ex/ex-incorrect "Bad challenge response" {})))
  true)

(defn random
  [challenges]
  (-> challenges keys rand-nth))
