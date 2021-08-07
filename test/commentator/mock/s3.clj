(ns commentator.mock.s3
  (:require [commentator.store :as store]
            [spy.protocol :as protocol]))


(defrecord StoreMock [exists? get-resource save-resource delete-resource]
  store/IStoreOperator
  (exists? [this website resource-name]
    (exists? website resource-name))
  (get-resource [this website resource-name]
    (get-resource website resource-name))
  (save-resource [this website resource-name content]
    (save-resource resource-name content))
  (delete-resource [this website resource-name]
    (delete-resource resource-name)))

(defn store-mock
  "Creates a mock for the store component."
  [config]
  (protocol/spy store/IStoreOperator
                (map->StoreMock config)))
