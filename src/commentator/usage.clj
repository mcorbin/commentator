(ns commentator.usage
  (:require [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [commentator.rate-limit :as rl]
            [commentator.store :as store]
            [corbihttp.log :as log])
  (:import java.util.concurrent.ScheduledThreadPoolExecutor
           java.util.concurrent.TimeUnit
           java.time.LocalDate
           java.time.format.DateTimeFormatter))

(defprotocol IWebsiteUsage
  (new-request [this request] "Add this request to the website usage component"))

(def ^DateTimeFormatter formatter (DateTimeFormatter/ofPattern "yyyy/MM/dd"))

(defn now
  []
  (.format (LocalDate/now) formatter))

(defn add-request
  [cache request]
  (let [ip (rl/source-ip request)
        website (get-in request [:all-params :website])
        path (get-in request [:all-params :path])
        day (now)]
    (if (get-in cache [website day path ip])
      (update-in cache [website day path ip] inc)
      (assoc-in cache [website day path ip] 1))))

(defn resource-name
  [day]
  (str "usage/" day))

(defn sync-cache
  [cache s3]
  (try
    (doseq [[website website-usage] @cache]
      (doseq [[day content] website-usage]
        (log/infof {}
                   "sync usage cache for %s - %s"
                   website
                   day)
        (store/save-resource s3
                             website
                             (resource-name day)
                             (json/generate-string content))))
    (catch Exception e
      (log/error {} e "fail to sync cache"))))

(defn load-cache
  [cache s3 website]
  (log/info {:website website} "loading usage cache")
  (let [day (now)
        day-resource (resource-name day)]
    (when (store/exists? s3 website day-resource)
      (swap! cache assoc-in [website day] (-> (store/get-resource s3 website day-resource)
                                              (json/parse-string))))))

(defn purge-cache
  [cache]
  (log/info {} "purge usage cache")
  (try
    (->> (map (fn [[website website-cache]]
                [website (select-keys website-cache [(now)])])
              cache)
         (into {}))
    (catch Exception e
      (log/error {} e "fail to purge cache"))))

(defrecord WebsiteUsage [websites s3 cache executor]
  component/Lifecycle
  (start [this]
    (let [executor (ScheduledThreadPoolExecutor. 1)
          c (atom {})]
      (doseq [website websites]
        (load-cache c s3 website))
      (.scheduleWithFixedDelay executor
                               ^Runnable (fn []
                                           (sync-cache c s3))
                               20
                               60
                               TimeUnit/SECONDS)
      (.scheduleWithFixedDelay executor
                               ^Runnable (fn []
                                           (swap! c purge-cache))
                               60
                               14400
                               TimeUnit/SECONDS)
      (assoc this :cache c :executor executor)))

  (stop [this]
    (when executor
      (.shutdown executor)
      (.awaitTermination executor
                         20
                         TimeUnit/SECONDS)
      ;; sync one last time
      (sync-cache cache s3))
    (assoc this :cache nil :executor nil))
  IWebsiteUsage
  (new-request [_ request]
    (swap! cache add-request request)))
