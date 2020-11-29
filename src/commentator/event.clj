(ns commentator.event
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [commentator.log :as log]
            [commentator.store :as store]
            [exoscale.coax :as coax]
            [exoscale.ex :as ex])
  (:import java.util.UUID))

(def event-file-name "events.json")

(s/def ::type #{:new-comment})
(s/def ::timestamp pos-int?)
(s/def ::id uuid?)

(s/def ::event (s/keys :req-un [::type ::timestamp ::id]))
(s/def ::events (s/coll-of ::event))

(defn new-comment
  "Generates an event for a new comment"
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
  (delete-event [this event-id]))

(defrecord EventManager [s3 lock]
  IEventManager
  (add-event [this event]
    (locking lock
      (let [events (list-events this)]
        (store/save-resource s3
                             event-file-name
                             (-> (conj events event)
                                 json/generate-string))
        (log/info {:event-id (:id event)
                   :event-type (:type event)}
                  (format "publish event %s" (:id event)))
        true)))
  (list-events [this]
    (if (store/exists? s3 event-file-name)
      (coax/coerce
       ::events
       (-> (store/get-resource s3 event-file-name)
           (json/parse-string true)
           vec))
      []))
  (delete-event [this event-id]
    (locking lock
      (if (store/exists? s3 event-file-name)
        (let [events (list-events this)
              filtered (remove #(= (:id %) event-id) events)]
          (when (= (count events)
                   (count filtered))
            (throw (ex/ex-not-found (format "Event %s not found"
                                            event-id)
                                    {:event-id event-id})))
          (store/save-resource s3
                               event-file-name
                               (json/generate-string filtered))
          (log/info {:event-id event-id}
                    (format "Event %s deleted" event-id)))
        (throw (ex/ex-not-found (format "Event %s not found"
                                        event-id)
                                {:event-id event-id})))))
  component/Lifecycle
  (start [this]
    (assoc this :lock (Object.)))
  (stop [this]
    (assoc this :lock false)))
