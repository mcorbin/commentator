(ns commentator.event
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [commentator.log :as log]
            [commentator.store :as store]
            [exoscale.coax :as coax])
  (:import java.util.UUID))

(def event-file-name "events.json")

(s/def ::type #{:new-comment})
(s/def ::timestamp pos-int?)
(s/def ::id uuid?)

(s/def ::event (s/keys :req-un [::type ::timestamp ::id]))
(s/def ::events (s/coll-of ::event))

(defn new-comment
  [article id]
  {:timestamp (System/currentTimeMillis)
   :id (UUID/randomUUID)
   :article article
   :message (format "New comment %s on article %s" id article)
   :comment-id id
   :type :new-comment})

(defprotocol IEventManager
  (add-event [this event])
  (list-events [this])
  (purge [this min-date]))

(defrecord EventManager [s3]
  IEventManager
  (add-event [this event]
    (let [events (list-events this)]
      (store/save-resource s3
                           event-file-name
                           (-> (conj events event)
                               json/generate-string))
      (log/info {:event-id (:id event)
                 :event-type (:type event)}
                (format "publish event %s" (:id event)))
      true))
  (list-events [this]
    (if (store/exists? s3 event-file-name)
      (coax/coerce
       ::events
       (-> (store/get-resource s3 event-file-name)
           (json/parse-string true)
           vec))
      []))
  (purge [this min-date]

    ))
