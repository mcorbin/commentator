(ns commentator.event
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [corbihttp.log :as log]
            [commentator.lock :as lock]
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
  (add-event [this website event])
  (list-events [this website])
  (delete-event [this website event-id]))

(defrecord EventManager [s3 lock]
  IEventManager
  (add-event [this website event]
    (locking (lock/get-lock lock website)
      (let [events (list-events this website)]
        (store/save-resource s3
                             website
                             event-file-name
                             (-> (conj events event)
                                 json/generate-string))
        (log/info {:event-id (:id event)
                   :event-type (:type event)}
                  (format "publish event %s" (:id event)))
        true)))
  (list-events [_ website]
    (if (store/exists? s3 website event-file-name)
      (coax/coerce
       ::events
       (-> (store/get-resource s3 website event-file-name)
           (json/parse-string true)
           vec))
      []))
  (delete-event [this website event-id]
    (locking (lock/get-lock lock website)
      (if (store/exists? s3 website event-file-name)
        (let [events (list-events this website)
              filtered (remove #(= (:id %) event-id) events)]
          (when (= (count events)
                   (count filtered))
            (throw (ex/ex-info (format "Event %s not found"
                                       event-id)
                               [::not-found [:corbi/user ::ex/not-found]]
                               {:event-id event-id})))
          (store/save-resource s3
                               website
                               event-file-name
                               (json/generate-string filtered))
          (log/info {:event-id event-id}
                    (format "Event %s deleted" event-id)))
        (throw (ex/ex-info (format "Event %s not found"
                                   event-id)
                           [::not-found [:corbi/user ::ex/not-found]]
                           {:event-id event-id}))))))
