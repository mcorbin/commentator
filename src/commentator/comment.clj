(ns commentator.comment
  (:require [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [corbihttp.log :as log]
            [commentator.cache :as c]
            [commentator.lock :as lock]
            [commentator.spec :as spec]
            [commentator.store :as store]
            [exoscale.coax :as coax]
            [exoscale.ex :as ex]))

(def COMMENT_MAX_SIZE 10000)
;; 24 hours
(def cache-ttl (* 1000 60 60 24))

(s/def ::id uuid?)
(s/def ::content (s/and ::spec/non-empty-string
                        #(< (count %) 10000)))
(s/def ::author ::spec/non-empty-string)
(s/def ::timestamp pos-int?)
(s/def ::approved boolean?)
(s/def ::author-website (s/and ::spec/non-empty-string
                               #(< (count %) 128)))

(s/def ::comment (s/keys :req-un [::id ::content ::author ::timestamp ::approved]
                         :opt-un [::author-website]))
(s/def ::comments (s/coll-of ::comment))

(defprotocol ICommentManager
  (article-exists? [this website article] "Checks if an article resource exists.")
  (for-article
    [this website article]
    [this website article all?]
    "Returns all comments for an article. The all? parameter can be set to true in order to return all comments, approved or not")
  (delete-article [this website article] "Delete all comments for an article")
  (approve-comment [this website article comment-id] "Approve a comment for an article")
  (add-comment [this website article comment] "Add a comment for an article")
  (delete-comment [this website article comment-id] "Delete a comment for an article")
  (get-comment [this website article comment-id] "Get a comment by ID for an article"))

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
  (when (and (:author comment) (:content comment))
    (-> (update comment :author escape-html)
        (update :content escape-html))))

(defn article-file-name
  [article]
  (str article ".json"))

(defn allowed?
  [allowed-articles website article]
  (if-not allowed-articles
    ;; allowed-articles is optional
    true
    (let [articles (get allowed-articles website)]
      (if-not (and articles (articles article))
        (throw (ex/ex-info (format "Invalid article %s" article)
                           [::invalid [:corbi/user ::ex/incorrect]]))
        true))))

(defrecord CommentManager [auto-approve allowed-articles s3 lock cache]
  ICommentManager
  (article-exists? [_ website article]
    (store/exists? s3 website (article-file-name article)))

  (for-article [this website article]
    (for-article this website article false))

  (for-article [this website article all?]
    (allowed? allowed-articles website article)
    (if (article-exists? this website article)
      (let [{:keys [comments from-cache]}
            (if-let [cache-comments (c/lookup cache website article)]
              {:comments cache-comments :from-cache true}
              {:comments
               (coax/coerce
                ::comments
                (-> (store/get-resource s3 website (article-file-name article))
                    (json/parse-string true)))
               :from-cache false})]
        (when-not from-cache
          ;; update the cache if the value was retrieved from s3
          (c/miss cache website article comments))
        (if all?
          (vec comments)
          (vec (filter :approved comments))))
      []))

  (delete-article [this website article]
    (log/infof {}
               "Delete comments for %s/%s"
               website
               article)
    (locking (lock/get-lock lock (str website "/" article))
      (when (article-exists? this website article)
        (c/evict cache website article)
        (store/delete-resource s3 website (article-file-name article)))))

  (add-comment [this website article comment]
    (allowed? allowed-articles website article)
    (locking (lock/get-lock lock (str website "/" article))
      (if (article-exists? this website article)
        (let [comments (for-article this website article true)]
          (log/infof {}
                     "Adding comment %s for %s/%s"
                     (:id comment)
                     website
                     article)
          (store/save-resource s3
                               website
                               (article-file-name article)
                               (-> (conj comments
                                         (assoc comment :approved auto-approve))
                                   json/generate-string)))
        ;; first comment for this article
        (store/save-resource s3
                             website
                             (article-file-name article)
                             (-> [comment]
                                 json/generate-string)))
      (c/evict cache website article))
    (log/info {:comment-id (:id comment)
               :article article}
              (format "New comment %s for article %s" (:id comment) article)))

  (approve-comment [this website article comment-id]
    (locking (lock/get-lock lock (str website "/" article))
      (if (article-exists? this website article)
        (let [{:keys [found comments]}
              (->> (for-article this website article true)
                   (reduce (fn [state comment]
                             (if (= (:id comment) comment-id)
                               (-> (assoc state :found true)
                                   (update :comments conj (assoc comment :approved true)))
                               (update state :comments conj comment)))
                           {:found false
                            :comments []}))]
          (when-not found
            (throw (ex/ex-info (format "Comment %s not found for article %s"
                                       comment-id
                                       article)
                               [::not-found [:corbi/user ::ex/not-found]]
                               {:article article
                                :comment-id comment-id})))
          (store/save-resource s3
                               website
                               (article-file-name article)
                               (json/generate-string comments))
          (c/evict cache website article)
          (log/info {:comment-id comment-id
                     :article article}
                    (format "Comment %s for article %s approved" comment-id article)))
        (throw (ex/ex-info (format "No comment for article %s"
                                   article)
                           [::not-found [:corbi/user ::ex/not-found]]
                           {:article article
                            :comment-id comment-id})))))

  (delete-comment [this website article comment-id]
    (locking (lock/get-lock lock (str website "/" article))
      (if (article-exists? this website article)
        (let [comments (for-article this website article true)
              filtered (remove #(= (:id %) comment-id) comments)]
          (when (= (count comments)
                   (count filtered))
            (throw (ex/ex-info (format "Comment %s not found for article %s"
                                       comment-id
                                       article)
                               [::not-found [:corbi/user ::ex/not-found]]
                               {:comment-id comment-id
                                :article article})))
          (log/infof  {}
                      "deleting comment %s for %s/%s"
                      comment-id
                      website
                      article)
          (store/save-resource s3
                               website
                               (article-file-name article)
                               (json/generate-string filtered))
          (c/evict cache website article)
          (log/info {:comment-id comment-id
                     :article article}
                    (format "Comment %s for article %s deleted" comment-id article)))
        (throw (ex/ex-info (format "No comment for article %s"
                                   article)
                           [::not-found [:corbi/user ::ex/not-found]]
                           {:article article
                            :comment-id comment-id})))))

  (get-comment [this website article comment-id]
    (if (article-exists? this website article)
      (if-let [comment (->> (for-article this website article true)
                            (filter #(= (:id %) comment-id))
                            first)]
        comment
        (throw (ex/ex-info (format "Comment %s not found for article %s"
                                   comment-id
                                   article)
                           [::not-found [:corbi/user ::ex/not-found]]
                           {:article article
                            :comment-id comment-id})))
      (throw (ex/ex-info (format "No comment for article %s"
                                 article)
                         [::not-found [:corbi/user ::ex/not-found]]
                         {:article article
                          :comment-id comment-id})))))
