(ns commentator.spec
  (:require [clojure.spec.alpha :as s]))

(s/def ::non-empty-string (s/and string? not-empty))
(s/def ::keyword keyword?)
