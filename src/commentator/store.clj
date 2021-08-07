(ns commentator.store
  "The store component is responsible for storing resources."
  (:require [amazonica.aws.s3 :as s3]
            [exoscale.cloak :as cloak])
  (:import java.io.ByteArrayInputStream
           org.apache.commons.codec.digest.DigestUtils
           org.apache.commons.codec.binary.Base64
           org.apache.commons.io.IOUtils))

(defprotocol IStoreOperator
  (exists? [this website resource-name] "Checks if a retourne exists")
  (get-resource [this website resource-name] "Get a retourne by name")
  (save-resource [this website resource-name content] "Save a resource")
  (delete-resource [this website resource-name] "Delete a resource"))

(defn exists?-s3
  "Checks if a file exists in s3"
  [credentials bucket resource-name]
  (s3/does-object-exist (cloak/unmask credentials)
                        bucket
                        resource-name))

(defn get-resource-from-3
  "Get a file from s3."
  [credentials bucket resource-name]
  (String.
   (IOUtils/toByteArray
    (:object-content
     (s3/get-object (cloak/unmask credentials)
                    :bucket-name bucket
                    :key resource-name)))))

(defn delete-resource-from-s3
  "Delete a file from s3"
  [credentials bucket resource-name]
  (s3/delete-object (cloak/unmask credentials)
                    :bucket-name bucket
                    :key resource-name))

(defn save-on-s3
  "Creates a new S3 file."
  [credentials bucket resource-name ^String content]
  (let [bytes (.getBytes content "UTF-8")
        input-stream (ByteArrayInputStream. bytes)
        digest (DigestUtils/md5 bytes)]
    (s3/put-object (cloak/unmask credentials)
                   :bucket-name bucket
                   :key resource-name
                   :input-stream input-stream
                   :metadata {:content-length (alength bytes)
                              :content-md5 (String. (Base64/encodeBase64
                                                     digest))})))

(defn bucket-name
  [bucket-prefix website]
  (str bucket-prefix website))

(defrecord S3 [credentials bucket-prefix]
  IStoreOperator
  (exists? [this website resource-name]
    (exists?-s3 credentials (bucket-name bucket-prefix website) resource-name))
  (get-resource [this website resource-name]
    (get-resource-from-3 credentials
                         (bucket-name bucket-prefix website)
                         resource-name))
  (save-resource [this website resource-name content]
    (save-on-s3 credentials
                (bucket-name bucket-prefix website)
                resource-name
                content))
  (delete-resource [this website resource-name]
    (delete-resource-from-s3 credentials
                             (bucket-name bucket-prefix website)
                             resource-name)))
