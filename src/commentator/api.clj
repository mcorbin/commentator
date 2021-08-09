(ns commentator.api
  (:require [clojure.spec.alpha :as s]
            [commentator.comment :as comment]
            [commentator.config :as config]
            [commentator.event :as event]
            [commentator.handler :as handler]
            [commentator.spec :as spec]))

(s/def ::challenge ::spec/keyword)
(s/def ::answer ::spec/non-empty-string)
(s/def ::comment-id ::comment/id)
(s/def ::article ::spec/non-empty-string)

(s/def :comment/new (s/keys :req-un [::article
                                     ::comment/author
                                     ::comment/email
                                     ::comment/content
                                     ::comment/author-website
                                     ::challenge
                                     ::config/website
                                     ::answer]))
(s/def :comment/get (s/keys :req-un [::article
                                     ::config/website
                                     ::comment-id]))
(s/def :comment/for-article (s/keys :req-un [::article
                                             ::config/website]))
(s/def :comment/approve (s/keys :req-un [::article ::comment-id ::config/website]))
(s/def :comment/delete (s/keys :req-un [::article ::comment-id ::config/website]))
(s/def :comment/delete-article (s/keys :req-un [::article ::config/website]))
(s/def :comment/admin-for-article (s/keys :req-un [::article ::config/website]))

(s/def :event/delete (s/keys :req-un [::event/id ::config/website]))
(s/def :event/list (s/keys :req-un [::config/website]))

(def dispatch-map
  {:comment/new {:path ["api/v1/comment/" :website "/":article #"/?"]
                 :handler-fn handler/new-comment
                 :spec :comment/new
                 :method :post}
   :comment/get {:path ["api/admin/comment/":website "/" :article "/" :comment-id #"/?"]
                 :handler-fn handler/get-comment
                 :spec :comment/get
                 :method :get}
   :comment/for-article {:path ["api/v1/comment/" :website "/" :article #"/?"]
                         :spec :comment/for-article
                         :method :get
                         :handler-fn handler/comments-for-article}
   :comment/approve {:path ["api/admin/comment/" :website "/" :article "/" :comment-id #"/?"]
                     :handler-fn handler/approve-comment
                     :spec :comment/approve
                     :method :post}
   :comment/delete {:path ["api/admin/comment/" :website "/" :article "/" :comment-id #"/?"]
                    :method :delete
                    :spec :comment/delete
                    :handler-fn handler/delete-comment}
   :comment/delete-article {:path ["api/admin/comment/" :website "/" :article #"/?"]
                            :handler-fn handler/delete-article-comments
                            :spec :comment/delete-article
                            :method :delete}
   :comment/admin-for-article {:path ["api/admin/comment/" :website "/" :article #"/?"]
                               :handler-fn handler/admin-for-article
                               :spec :comment/admin-for-article
                               :method :get}
   :challenge/random {:path [#"api/v1/challenge/?"]
                      :handler-fn handler/random-challenge
                      :method :get}
   :event/list {:path [#"api/admin/event/" :website #"/?"]
                :method :get
                :spec :event/list
                :handler-fn handler/list-events}
   :event/delete {:path ["api/admin/event/" :website "/" :event-id #"/?"]
                  :spec :event/delete
                  :method :delete
                  :handler-fn handler/delete-event}
   :system/healthz {:path #"healthz/?"
                    :method :get
                    :handler-fn handler/healthz}})
