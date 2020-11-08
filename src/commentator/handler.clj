(ns commentator.handler
  "HTTP Handler component.
  This component is responsible from executing the actions binded to HTTP handlers."
  (:require [commentator.event :as ce]
            [commentator.challenge :as challenge]
            [commentator.comment :as cc]
            [commentator.log :as log]
            [commentator.spec :as spec]
            [exoscale.coax :as coax]
            [exoscale.ex :as ex])
  (:import java.util.UUID))

(defprotocol IHandler
  (new-comment [this request] "Creates a new comment")
  (get-comment [this request] "Get a specific comment")
  (comments-for-article [this request all?] "Get a comment for a specific article. all? to false will return only comments which are approved")
  (delete-comment [this request] "Delete a comment")
  (delete-article-comments [this request] "Delete all comments for an article")
  (approve-comment [this request] "Approve a comment")
  (list-events [this request] "List all events")
  (healthz [this request] "Healthz endpoint")
  (metrics [this request] "Metric endpoint")
  (not-found [this request] "Not found response"))

(defn req->article
  [request]
  (get-in request [:route-params :article]))

(defn req->comment-id
  [request]
  (let [comment-id (->> (get-in request [:route-params :comment-id])
                        (coax/coerce ::cc/id))]
    (ex/assert-spec-valid ::cc/id comment-id)
    comment-id))

(defrecord Handler [comment-manager event-manager challenges]
  IHandler
  (new-comment [this request]
    (let [article (req->article request)
          body (:body request)
          comment (coax/coerce
                   ::cc/comment
                   (-> (merge (select-keys body [:content :author])
                              {:id (UUID/randomUUID)
                               :approved false
                               :timestamp (System/currentTimeMillis)})))
          challenge (coax/coerce ::spec/keyword (:challenge body))
          answer (:answer body)]
      (ex/assert-spec-valid ::cc/comment comment)
      (ex/assert-spec-valid ::spec/keyword challenge)
      (ex/assert-spec-valid ::spec/non-empty-string answer)
      (challenge/verify challenges challenge answer)
      (cc/add-comment comment-manager article comment)
      (future (try (ce/add-event event-manager (ce/new-comment article
                                                               (:id comment)))
                   (catch Exception e
                     (log/error (log/req-ctx request)
                                e
                                (format "fail to send event for new comment %s on article %s"
                                        (:id comment)
                                        article)))))
      {:status 201
       :body {:message "Comment added"}}))

  (get-comment [this request]
    (let [article (req->article request)
          comment-id (req->comment-id request)]
      {:status 200
       :body (cc/get-comment comment-manager article comment-id)}))

  (comments-for-article [this request all?]
    (let [article (req->article request)]
      {:status 200
       :body (cc/for-article comment-manager article all?)}))

  (delete-comment [this request]
    (let [article (req->article request)
          comment-id (req->comment-id request)]
      (cc/delete-comment comment-manager article comment-id)
      {:status 200 :body {:message "Comment deleted"}}))

  (delete-article-comments [this request]
    (let [article (req->article request)]
      (cc/delete-article comment-manager article)
      {:status 200 :body {:message "Comments deleted"}}))

  (approve-comment [this request]
    (let [article (req->article request)
          comment-id (req->comment-id request)]
      (cc/approve-comment comment-manager article comment-id)
      {:status 200 :body {:message "Comment approved"}}))

  (list-events [this request]
    {:status 200
     :body (ce/list-events event-manager)})

  ;; TODO
  (metrics [this request]
    {:status 200
     :body ""})

  (healthz [this request]
    {:status 200
     :body {:message "ok"}})

  (not-found [this request]
    {:status 404
     :body {:error "not found"}}))
