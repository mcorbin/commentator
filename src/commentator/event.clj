(ns commentator.event
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [commentator.store :as store]))

(def event-file-name "events.json")

(s/def ::type #{:new-comment})
(s/def ::date inst?)

(s/def ::event (s/keys :req-un [::type ::date]))

(defprotocol IEventManager
  (add-event [this event])
  (list-events [this])
  (purge [this min-date]))

(defrecord EventManager [s3]
  IEventManager
  (add-event [this event]
    (let [events (-> (store/get-resource s3 event-file-name)
                     (json/parse-string true))]
      (store/save-resource s3
                           event-file-name
                           (-> (conj events event)
                               json/generate-string))))
  (list-events [this]
    (-> (store/get-resource s3 event-file-name)
        (json/parse-string true)))
  (purge [this min-date]
    
    ))
