(ns commentator.mock.s3
  (:require [commentator.store :as store]
            [spy.protocol :as protocol]))


(defrecord StoreMock [exists? get-resource save-resource delete-resource]
  store/IStoreOperator
  (exists? [this resource-name]
    (exists? resource-name))
  (get-resource [this resource-name]
    (get-resource resource-name))
  (save-resource [this resource-name content]
    (save-resource resource-name content))
  (delete-resource [this resource-name]
    (delete-resource resource-name)))

(defn store-mock
  "Creates a mock for the store component."
  [config]
  (protocol/spy store/IStoreOperator
                (map->StoreMock config)))
