(ns commentator.usage-test
  (:require [cheshire.core :as json]
            [clojure.test :refer :all]
            [commentator.mock.s3 :as ms]
            [commentator.usage :as usage]
            [spy.assert :as assert]
            [spy.protocol :as protocol]))

(deftest purge-cache-test
  (is (= {}
         (usage/purge-cache {})))
  (is (= {"mcorbin-fr" {}}
         (usage/purge-cache {"mcorbin-fr" {"20210201" {"/foo" {"10.0.0.1" 1}}}})))
  (is (= {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1}}}}
         (usage/purge-cache {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1}}}})))
  (is (= {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1}}}
          "foo.localhost" {(usage/now) {"/bar" {"10.0.0.2" 3}}}}
         (usage/purge-cache {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1}}
                                           "20210201" {"/foo" {"10.0.0.1" 1}}}
                             "foo.localhost" {(usage/now) {"/bar" {"10.0.0.2" 3}}
                                              "20210201" {"/foo" {"10.0.0.1" 1}}}}))))

(deftest add-request-test
  (is (= {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1}}}}
         (usage/add-request
          {}
          {:uri "/bar"
           :all-params {:website "mcorbin-fr" :path "/foo"}
           :remote-addr "10.0.0.1"})))
    (is (= {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 2}}}}
         (usage/add-request
          {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1}}}}
          {:uri "/foo"
           :all-params {:website "mcorbin-fr" :path "/foo"}
           :remote-addr "10.0.0.1"})))
  (is (= {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1
                                             "10.0.0.2" 1}}}}
         (usage/add-request
          {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1}}}}
          {:uri "/foo"
           :all-params {:website "mcorbin-fr" :path "/foo"}
           :remote-addr "10.0.0.2"})))
  (is (= {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1
                                             "10.0.0.2" 2}}}}
         (usage/add-request
          {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1
                                              "10.0.0.2" 1}}}}
          {:uri "/foo"
           :all-params {:website "mcorbin-fr" :path "/foo"}
           :remote-addr "10.0.0.2"})))
  (is (= {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1
                                             "10.0.0.2" 1}
                                     "/bar" {"10.0.0.3" 1}}}}
         (usage/add-request
          {"mcorbin-fr" {(usage/now) {"/foo" {"10.0.0.1" 1
                                              "10.0.0.2" 1}}}}
          {:uri "/foo"
           :all-params {:website "mcorbin-fr" :path "/bar"}
           :remote-addr "10.0.0.3"}))))

(deftest sync-cache-test
  (let [store (ms/store-mock {:save-resource (constantly true)})
        now (usage/now)
        cache (atom {"mcorbin-fr" {now {"/foo" {"10.0.0.1" 1
                                                "10.0.0.2" 1}}}})]
    (usage/sync-cache cache store)
    (assert/called-with? (:save-resource (protocol/spies store))
                         store
                         "mcorbin-fr"
                         (usage/resource-name now)
                         (json/generate-string {"/foo" {"10.0.0.1" 1
                                                        "10.0.0.2" 1}}))))
