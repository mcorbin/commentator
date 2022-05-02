(ns commentator.api
  (:require [clojure.spec.alpha :as s]
            [commentator.comment :as comment]
            [commentator.config :as config]
            [commentator.event :as event]
            [commentator.handler :as handler]
            [commentator.spec :as spec]))

(s/def ::answer ::spec/non-empty-string)
(s/def ::comment-id ::comment/id)
(s/def ::article ::spec/non-empty-string)
(s/def ::timestamp pos-int?)
(s/def ::signature ::spec/non-empty-string)

(s/def ::path ::spec/non-empty-string)
(s/def :usage/add (s/keys :req-un [::config/website
                                   ::path]))

(s/def :comment/new (s/keys :req-un [::article
                                     ::comment/author
                                     ::comment/content
                                     ::config/website
                                     ::answer
                                     ::timestamp
                                     ::signature]
                            :opt-un [::comment/author-website]))
(s/def :comment/get (s/keys :req-un [::article
                                     ::config/website
                                     ::comment-id]))
(s/def :comment/for-article (s/keys :req-un [::article
                                             ::config/website]))
(s/def :comment/approve (s/keys :req-un [::article ::comment-id ::config/website]))
(s/def :comment/delete (s/keys :req-un [::article ::comment-id ::config/website]))
(s/def :comment/delete-article (s/keys :req-un [::article ::config/website]))
(s/def :comment/admin-for-article (s/keys :req-un [::article ::config/website]))
(s/def :challenge/random (s/keys :req-un [::article ::config/website]))

(s/def :event/delete (s/keys :req-un [::event/id ::config/website]))
(s/def :usage/day ::spec/non-empty-string)
(s/def :usage/month ::spec/non-empty-string)
(s/def :usage/year ::spec/non-empty-string)
(s/def :usage/get-for-day (s/keys :req-un [::config/website
                                           :usage/day
                                           :usage/month
                                           :usage/year]))
(s/def :event/list (s/keys :req-un [::config/website]))

(def router
  [["/api/v1/comment/:website/:article" {:post {:spec :comment/new
                                                :handler handler/new-comment}
                                         :get {:spec :comment/for-article
                                               :handler handler/comments-for-article}}]
   ["/api/v1/usage/:website" {:post {:spec :usage/add
                                     :handler handler/usage}}]
   ["/api/admin/comment/:website/:article" {:get {:handler handler/admin-for-article
                                                  :auth true
                                                  :spec :comment/admin-for-article}
                                            :delete {:spec :comment/delete-article
                                                     :auth true
                                                     :handler handler/delete-article-comments}}]
   ["/api/admin/comment/:website/:article/:comment-id" {:get {:spec :comment/get
                                                              :auth true
                                                              :handler handler/get-comment}
                                                        :delete {:spec :comment/delete
                                                                 :auth true
                                                                 :handler handler/delete-comment}
                                                        :post {:spec :comment/approve
                                                               :auth true
                                                               :handler handler/approve-comment}}]
   ["/api/v1/challenge/:website/:article" {:get {:handler handler/random-challenge
                                                 :spec :challenge/random}}]
   ["/api/admin/event/:website" {:get {:spec :event/list
                                       :auth true
                                       :handler handler/list-events}}]
   ["/api/admin/event/:website/:id" {:delete {:spec :event/delete
                                              :auth true
                                              :handler handler/delete-event}}]
   ["/api/admin/usage/:website/:year/:month/:day" {:delete {:spec :usage/get-for-day>
                                                            :auth true
                                                            :handler handler/get-usage-for-day}}]
   ["/healthz" {:get {:handler handler/healthz}}]])
