(ns commentator.error
  (:require [commentator.log :as log]
            [cheshire.core :as json]
            [clojure.string :as string]
            [exoscale.ex :as ex]))

(def default-msg
  "Internal error.")

(def default-problem-map
  {})

(defn last-keyword
  [coll]
  (-> (filter keyword? coll)
      last))

(defn problem->message
  "Turn an explain-data map into a human-friendly message."
  [problem problem-map]
  (let [{:keys [in pred via val]} problem
        spec (last-keyword via)
        field (last-keyword in)
        problem-map (merge default-problem-map problem-map)
        value (when val (json/generate-string val))]
    (or

     ;; try to get an error message by a spec from a dict
     (when spec
       (when-let [message (get problem-map spec)]
         (if field
           (format "field %s: %s" (name field)  message)
           ;; return the invalid value if the field is empty
           (if value
             (format "invalid value %s: %s" value message)
             message))))

     ;; missing field, but :in exists.
     ;; the field is missing in the :in map
     ;; the predicate would be a lazy seq of:
     ;; (clojure.core/fn [%] (clojure.core/contains? % :field))
     (when (and field
                (seq? pred)
                (> (count pred) 1)
                (seq? (last pred))
                (> (count (last pred)) 0)
                (= 'clojure.core/contains? (-> pred
                                               last
                                               first)))
       (format "field %s missing in %s"
               (-> pred last last name)
               (name field)))

     ;; no a message in a dict, but at least specify a field
     (when field
       (format "field %s is incorrect" (name field)))

     ;;
     ;; A case of a missing field: `:in` would be empty,
     ;; the predicate would be a lazy seq of:
     ;; (clojure.core/fn [%] (clojure.core/contains? % :field))
     ;; Try to get the nested field name.
     ;;
     (when (seq? pred)
       (when-let [field (-> pred last last)]
         (format "field %s is missing" (name field))))

     ;; :via is not a spec, return the invalid parameter
     (when value
       (format "invalid value %s" value))

     ;; default error message
     "invalid parameter")))

(defn problems->message
  [problems problem-map]
  (let [messages (map #(problem->message % problem-map) problems)]
    (with-out-str
      (println "Wrong input parameters:")
      (doseq [msg messages]
        (println (format " - %s"  msg))))))

(defn spec-ex->message
  [e]
  (->> (ex-data e)
       :explain-data
       :clojure.spec.alpha/problems
       (map #(problem->message % default-problem-map))
       (string/join ", ")))

(ex/derive ::ex/unavailable ::user)
(ex/derive ::ex/interrupted ::user)
(ex/derive ::ex/incorrect ::user)
(ex/derive ::ex/forbidden ::user)
(ex/derive ::ex/unauthorized ::user)
(ex/derive ::ex/unsupported ::user)
(ex/derive ::ex/not-found ::user)
(ex/derive ::ex/conflict ::user)
(ex/derive ::ex/fault ::user)
(ex/derive ::ex/busy ::user)

(def ex-type->status
  {::ex/unavailable 500
   ::ex/interrupted 500
   ::ex/incorrect 400
   ::ex/forbidden 403
   ::ex/unauthorized 401
   ::ex/unsupported 400
   ::ex/not-found 404
   ::ex/conflict 500
   ::ex/fault 500
   ::ex/busy 500})

(defn handle-spec-error
  [request ^Exception e]
  (let [message (spec-ex->message e)
        data (ex-data e)
        status 400]
    (log/error (merge (log/req-ctx request)
                      data)
               e
               "http error"
               status)
    {:status status
     :body {:error message}}))

(defn handle-user-error
  [request ^Exception e]
  (let [message (.getMessage e)
        data (ex-data e)
        status (get ex-type->status (:type data) 500)]
    (log/error (merge (log/req-ctx request)
                      data)
               e
               "http error"
               status)
    {:status status
     :body {:error message}}))

(defn handle-unexpected-error
  [request ^Exception e]
  (log/error (merge (log/req-ctx request)
                    (ex-data e))
             e "http error")
  ;(metric/http-errors request 500)
  {:status 500
   :body {:error default-msg}})
