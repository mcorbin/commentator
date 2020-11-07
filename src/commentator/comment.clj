(ns commentator.comment
  (:require [clojure.spec.alpha :as s]
            [cheshire.core :as json]
            [commentator.store :as store]
            [exoscale.coax :as coax]
            [exoscale.ex :as ex]))

(s/def ::id uuid?)
(s/def ::content string?)
(s/def ::author string?)
(s/def ::timestamp pos-int?)
(s/def ::approved boolean?)

(s/def ::comment (s/keys :req-un [::id ::content ::author ::timestamp ::approved]))
(s/def ::comments (s/coll-of ::comment))

(defprotocol ICommentManager
  (exists? [this article])
  (for-article
    [this article]
    [this article all?])
  (delete-article [this article])
  (approve-comment [this article comment-id])
  (add-comment [this article comment])
  (delete-comment [this article comment-id])
  (get-comment [this article comment-id]))

(defn article-file-name
  [article]
  (str article ".json"))

(defn allowed?
  [allowed-articles article]
  (when-not (allowed-articles article)
    (throw (ex/ex-incorrect (format "Invalid article %s" article) {}))))

(defrecord CommentManager [s3 auto-approve allowed-articles]
  ICommentManager
  (exists? [this article]
    (store/exists? s3 article))

  (for-article [this article]
    (allowed? allowed-articles article)
    (for-article this article false))

  (for-article [this article all?]
    (allowed? allowed-articles article)
    (let [comments (coax/coerce
                    ::comments
                    (-> (store/get-resource s3 (article-file-name article))
                        (json/parse-string true)))]
      (if all?
        comments
        (filter :approved comments))))

  (delete-article [this article]
    (when (exists? this article)
      (store/delete-resource s3 (article-file-name article))))

  (add-comment [this article comment]
    (allowed? allowed-articles article)
    (if (exists? this article)
      (let [comments (for-article this article)]
        (store/save-resource s3
                             (article-file-name article)
                             (-> (conj comments
                                       (assoc comment :approved auto-approve))
                                 json/generate-string)))
      ;; first comment for this article
      (store/save-resource s3
                           (article-file-name article)
                           (-> [comment]
                               json/generate-string))))

  (approve-comment [this article comment-id]
    (when (exists? this article)
      (let [{:keys [found comments]}
            (->> (for-article this article)
                 (reduce (fn [state comment]
                           (if (= (:id comment) comment-id)
                             (-> (assoc state :found true)
                                 (update :comments conj comment))
                             (update :comments conj comment)))
                         {:found false
                          :comments []}))]
        (when-not found
          (throw (ex/ex-not-found (format "Comment %s not found for article %s"
                                          comment-id
                                          article)
                                  {:article article
                                   :comment-id comment-id})))
        (store/save-resource s3
                             (article-file-name article)
                             (json/generate-string comments)))
      (throw (ex/ex-not-found (format "No comment for article %s"
                                      article)
                              {:article article
                               :comment-id comment-id}))))

  (delete-comment [this article comment-id]
    (when (exists? this article)
      (let [comments (-> (for-article this article)
                         (remove #(= (:id %) comment-id)))]
        (store/save-resource s3
                             (article-file-name article)
                             (json/generate-string comments)))
      (throw (ex/ex-not-found (format "No comment for article %s"
                                      article)
                              {:article article
                               :comment-id comment-id}))))

  (get-comment [this article comment-id]
    (if (exists? this article)
      (if-let [comment (-> (for-article this article)
                           (filter #(= (:id %) comment-id))
                           first)]
        comment
        (throw (ex/ex-not-found (format "Comment %s not found for article %s"
                                        comment-id
                                        article)
                                {:article article
                                 :comment-id comment-id})))
      (throw (ex/ex-not-found (format "No comment for article %s"
                                      article)
                              {:article article
                               :comment-id comment-id})))))
