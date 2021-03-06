(ns commentator.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [commentator.spec :as spec]
            [environ.core :as env]
            [exoscale.cloak :as cloak]
            [exoscale.ex :as ex]))

(s/def ::file (fn [path]
                (let [file (io/file path)]
                  (and (.exists file)
                       (.isFile file)))))

(s/def ::host ::spec/non-empty-string)
(s/def ::port pos-int?)
(s/def ::key ::file)
(s/def ::cert ::file)
(s/def ::cacert ::file)
(s/def ::http (s/keys :req-un [::host ::port]
                      :opt-un [::key ::cert ::cacert]))

(s/def ::token ::cloak/secret)
(s/def ::admin (s/keys :req-un [::token]))

(s/def ::access-key ::cloak/secret)
(s/def ::secret-key ::cloak/secret)
(s/def ::endpoint ::spec/non-empty-string)
(s/def ::bucket ::spec/non-empty-string)

(s/def ::auto-approve boolean?)
(s/def ::allowed-articles (s/coll-of ::spec/non-empty-string))
(s/def ::comment (s/keys :req-un [::auto-approve ::allowed-articles]))

(s/def ::store (s/keys :req-un [::access-key ::secret-key ::endpoint ::bucket]))

(s/def ::question ::spec/non-empty-string)
(s/def ::answer ::spec/non-empty-string)
(s/def ::challenge (s/keys :req-un [::question ::answer]))
(s/def ::challenges (s/map-of ::spec/keyword ::challenge))

(s/def ::prometheus ::http)

(s/def ::config (s/keys :req-un [::http ::admin ::store ::challenges]
                        :opt-un [::prometheus]))

(defmethod aero/reader 'secret
  [_ _ value]
  (cloak/mask value))

(defn load-config
  []
  (let [config (aero/read-config (env/env :commentator-configuration) {})]
    (if (s/valid? ::config config)
      config
      (throw (ex/ex-info
              "Invalid configuration"
              [::invalid [:corbi/user ::ex/incorrect]])))))
