(ns commentator.handler
  "HTTP Handler component.
  This component is responsible from executing the actions binded to HTTP handlers."
  (:require [commentator.event :as ce]
            [commentator.challenge :as challenge]
            [commentator.comment :as cc]
            [commentator.log :as log]
            [commentator.rate-limit :as rate-limit]
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
  (random-challenge [this request] "get a random challenge")
  (list-events [this request] "List all events")
  (delete-event [this request] "Delete a specific event")
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

(defn req->event-id
  [request]
  (let [comment-id (->> (get-in request [:route-params :event-id])
                        (coax/coerce ::ce/id))]
    (ex/assert-spec-valid ::ce/id comment-id)
    comment-id))

(defrecord Handler [comment-manager event-manager rate-limiter challenges]
  IHandler
  (new-comment [this request]
    (let [article (req->article request)
          body (:body request)
          comment (coax/coerce
                   ::cc/comment
                   (-> (merge (select-keys body [:content :author])
                              {:id (UUID/randomUUID)
                               :approved false
                               :timestamp (System/currentTimeMillis)})
                       cc/sanitize))
          challenge (coax/coerce ::spec/keyword (:challenge body))
          answer (:answer body)]
      (ex/assert-spec-valid ::cc/comment comment)
      (ex/assert-spec-valid ::spec/keyword challenge)
      (ex/assert-spec-valid ::spec/non-empty-string answer)
      (challenge/verify challenges challenge answer)
      (rate-limit/validate rate-limiter request)
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
       :body (cc/safe-for-article comment-manager article all?)}))

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

  (random-challenge [this request]
    (let [challenge (challenge/random challenges)]
      {:status 200
       :body {:name challenge
              :question (get-in challenges [challenge :question])}}))

  (list-events [this request]
    {:status 200
     :body (ce/list-events event-manager)})

  (delete-event [this request]
    (let [event-id (req->event-id request)]
      (ce/delete-event event-manager event-id)
      {:status 200
       :body {:message "Event deleted"}}))

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
