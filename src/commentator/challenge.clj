(ns commentator.challenge
  (:require [clojure.string :as string]
            [constance.comp :as constance]
            [exoscale.cloak :as cloak]
            [exoscale.ex :as ex])
  (:import org.apache.commons.codec.digest.HmacUtils))

(defprotocol IChallengeManager
  (random-challenge [this website article] "Returns a challenge")
  (verify [this payload] "Verifies a challenge"))

(defn hmac
  [secret ^String content]
  (.hmacHex (HmacUtils. org.apache.commons.codec.digest.HmacAlgorithms/HMAC_SHA_256
                        (cloak/unmask secret))
            content))

(defn signed-content
  [ts answer website article]
  (str ts "-" answer "-" website "-" article))

(defn sign
  [secret ^String answer ^String website ^String article]
  (let [ts (System/currentTimeMillis)
        content (signed-content ts (string/lower-case answer) website article)]
    {:timestamp ts
     :signature (hmac secret content)}))

(defn verify-challenge
  [secret payload ttl-seconds]
  (when (> (System/currentTimeMillis)
           (+ (:timestamp payload) (* 1000 ttl-seconds)))
    (throw (ex/ex-info "The challenge has expired"
                       [::bad-challenge [:corbi/user ::ex/incorrect]]
                       {})))
  (let [content (signed-content (:timestamp payload)
                                (string/lower-case (:answer payload))
                                (:website payload)
                                (:article payload))
        computed-signature (hmac secret content)]
    (when-not (constance/constant-string= computed-signature (:signature payload))
      (throw (ex/ex-info "Bad challenge response"
                         [::bad-challenge [:corbi/user ::ex/incorrect]]
                         {}))))
  true)


(defmulti random
  (fn [config _ _] (:type config)))

(defmethod random :questions
  [config website article]
  (let [challenge (-> config :questions rand-nth)
        signature (sign (:secret config) (:answer challenge) website article)]
    (assoc signature :question (:question challenge))))

(def operations [* + -])
(def mapping {* " * " + " + " - " - "})

(defmethod random :math
  [config website article]
  (let [n1 (rand-int 12)
        n2 (rand-int 12)
        op (rand-nth operations)
        challenge {:question (format "what is the result of: %d %s %d"
                                     n1
                                     (get mapping op)
                                     n2)
                   :answer (str (op n1 n2))}
        signature (sign (:secret config) (:answer challenge) website article)]
    (assoc signature :question (:question challenge))))

(defrecord ChallengeManager [config]
  IChallengeManager
  (random-challenge [_ website article]
    (random config website article))
  (verify [_ payload]
    (verify-challenge (:secret config) payload (:ttl config))))
