(ns commentator.store
  (:require [amazonica.aws.s3 :as s3]
            [exoscale.cloak :as cloak])
  (:import java.io.ByteArrayInputStream
           org.apache.commons.codec.digest.DigestUtils
           org.apache.commons.codec.binary.Base64
           org.apache.commons.io.IOUtils))

(defprotocol IStoreOperator
  (exists? [this resource-name])
  (get-resource [this resource-name])
  (save-resource [this resource-name content])
  (delete-resource [this resource-name]))

(defn file-key
  [bucket n]
  (str bucket "/" n))

(defn exists?-s3
  "Checks if a file exists in s3"
  [credentials bucket resource-name]
  (s3/does-object-exist (cloak/unmask credentials)
                        bucket
                        (file-key bucket resource-name)))

(defn get-resource-from-3
  "Get a file from s3."
  [credentials bucket resource-name]
  (IOUtils/toByteArray
   (:object-content
    (s3/get-object (cloak/unmask credentials)
                   :bucket-name bucket
                   :key (file-key bucket resource-name)))))

(defn delete-resource-from-s3
  "Delete a file from s3"
  [credentials bucket resource-name]
  (s3/delete-object (cloak/unmask credentials)
                    :bucket-name bucket
                    :key (file-key bucket resource-name)))

(defn save-on-s3
  "Creates a new S3 file."
  [credentials bucket resource-name ^String content]
  (let [bytes (.getBytes content "UTF-16")
        input-stream (ByteArrayInputStream. bytes)
        digest (DigestUtils/md5 bytes)]
    (s3/put-object (cloak/unmask credentials)
                   :bucket-name bucket
                   :key (file-key bucket resource-name)
                   :input-stream input-stream
                   :metadata {:content-length (alength bytes)
                              :content-md5 (String. (Base64/encodeBase64
                                                     digest))})))

(defrecord S3 [credentials bucket]
  IStoreOperator
  (exists? [this resource-name]
    (exists?-s3 credentials bucket resource-name))
  (get-resource [this resource-name]
    (get-resource-from-3 credentials bucket resource-name))
  (save-resource [this resource-name content]
    (save-on-s3 credentials bucket resource-name content))
  (delete-resource [this resource-name]
    (delete-resource-from-s3 credentials bucket resource-name)))
