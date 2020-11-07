(ns commentator.handler
  (:require [commentator.event :as ce]
            [commentator.comment :as cc]
            [exoscale.coax :as coax]
            [exoscale.ex :as ex])
  (:import java.util.UUID))

(defprotocol IHandler
  (new-comment [request])
  (get-comment [request])
  (comments-for-article [request])
  (delete-comment [request])
  (approve-comment [request])
  (list-events [request])
  (healthz [request])
  (metrics [request])
  (not-found [request]))

(defn req->article
  [request]
  (get-in request [:route-params :article]))

(defn req->comment-id
  [request]
  (let [comment-id (->> (get-in request [:route-params :comment-id])
                        (coax/coerce 'uuid?))]
    (ex/assert-spec-valid ::cc/id comment-id)
    comment-id))

(defrecord Handler [comment-manager event-manager]
  IHandler
  (new-comment [request]
    (let [article (req->article request)
          comment (coax/coerce
                   ::cc/comment
                   (-> (merge (:body request)
                              {:id (UUID/randomUUID)
                               :approved false
                               :timestamp (System/currentTimeMillis)})
                       (select-keys [:id :timestamp :author :content])))]
      {:status 201
       :body (cc/add-comment comment-manager article comment)}))

  (get-comment [request]
    (let [article (req->article request)
          comment-id (req->comment-id)]
      {:status 200
       :body (cc/get-comment comment-manager article comment-id)}))

  (comments-for-article [request]
    (let [article (req->article request)]
      {:status 200
       :body (cc/for-article comment-manager article)}))

  (delete-comment [request]
    (let [article (req->article request)
          comment-id (req->comment-id request)]
      (cc/delete-comment comment-manager article comment-id)))

  (approve-comment [request]
    (let [article (req->article request)
          comment-id (req->comment-id request)]
      (cc/approve-comment comment-manager article comment-id)))

  (list-events [request]
    {:status 200
     :body (ce/list-events event-manager)})

  (metrics [request])

  (healthz [request]
    {:status 200
     :body {:message "ok"}})

  (not-found [request]
    {:status 404
     :body {:error "not found"}}))
