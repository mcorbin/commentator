(ns commentator.comment
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [com.stuartsierra.component :as component]
            [commentator.log :as log]
            [commentator.spec :as spec]
            [commentator.store :as store]
            [exoscale.coax :as coax]
            [exoscale.ex :as ex]))

(def COMMENT_MAX_SIZE 10000)

(s/def ::id uuid?)
(s/def ::content (s/and ::spec/non-empty-string
                        #(< (:count % 10000))))
(s/def ::author ::spec/non-empty-string)
(s/def ::timestamp pos-int?)
(s/def ::approved boolean?)

(s/def ::comment (s/keys :req-un [::id ::content ::author ::timestamp ::approved]))
(s/def ::comments (s/coll-of ::comment))

(defprotocol ICommentManager
  (article-exists? [this article] "Checks if an article resource exists.")
  (for-article
    [this article]
    [this article all?]
    "Returns all comments for an article. The all? parameter can be set to true in order to return all comments, approved or not")
  (delete-article [this article] "Delete all comments for an article")
  (approve-comment [this article comment-id] "Approve a comment for an article")
  (add-comment [this article comment] "Add a comment for an article")
  (delete-comment [this article comment-id] "Delete a comment for an article")
  (get-comment [this article comment-id] "Get a comment by ID for an article"))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (.. ^String text
    (string/replace "&" "&amp;")
    (string/replace "<" "&lt;")
    (string/replace ">" "&gt;")
    (string/replace "\"" "&quot;")))

(defn sanitize
  [comment]
  (-> (update comment :author escape-html)
      (update :content escape-html)))

(defn article-file-name
  [article]
  (str article ".json"))

(defn allowed?
  [allowed-articles article]
  (when-not (allowed-articles article)
    (throw (ex/ex-incorrect (format "Invalid article %s" article) {})))
  true)

(defrecord CommentManager [auto-approve allowed-articles s3 lock]
  ICommentManager
  (article-exists? [this article]
    (store/exists? s3 (article-file-name article)))

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
        (vec comments)
        (vec (filter :approved comments)))))

  (delete-article [this article]
    (locking lock
      (when (article-exists? this article)
        (store/delete-resource s3 (article-file-name article)))))

  (add-comment [this article comment]
    (allowed? allowed-articles article)
    (locking lock
      (if (article-exists? this article)
        (let [comments (for-article this article true)]
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
    (log/info {:comment-id (:id comment)
               :article article}
              (format "New comment %s for article %s" (:id comment) article)))

  (approve-comment [this article comment-id]
    (locking lock
      (if (article-exists? this article)
        (let [{:keys [found comments]}
              (->> (for-article this article true)
                   (reduce (fn [state comment]
                             (if (= (:id comment) comment-id)
                               (-> (assoc state :found true)
                                   (update :comments conj (assoc comment :approved true)))
                               (update state :comments conj comment)))
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
                               (json/generate-string comments))
          (log/info {:comment-id comment-id
                     :article article}
                    (format "Comment %s for article %s approved" comment-id article)))
        (throw (ex/ex-not-found (format "No comment for article %s"
                                        article)
                                {:article article
                                 :comment-id comment-id})))))

  (delete-comment [this article comment-id]
    (locking lock
      (if (article-exists? this article)
        (let [comments (->> (for-article this article true)
                            (remove #(= (:id %) comment-id)))]
          ;; TODO: throw if not found
          (store/save-resource s3
                               (article-file-name article)
                               (json/generate-string comments))
          (log/info {:comment-id comment-id
                     :article article}
                    (format "Comment %s for article %s deleted" comment-id article)))
        (throw (ex/ex-not-found (format "No comment for article %s"
                                        article)
                                {:article article
                                 :comment-id comment-id})))))

  (get-comment [this article comment-id]
    (if (article-exists? this article)
      (if-let [comment (->> (for-article this article true)
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
                               :comment-id comment-id}))))
  component/Lifecycle
  (start [this]
    (assoc this :lock (Object.)))
  (stop [this]
    (assoc this :lock false)))
