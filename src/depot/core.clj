(ns depot.core
  (:use depot.xml
        [clojure.data.xml :only [lazy-parse]])
  (:require [clojure.string :as s]))

(defn group-path [group]
  (s/replace (name group) #"/." "/"))

(defn metadata [repo group artifact]
  (lazy-parse (format "%s/%s/%s/maven-metadata.xml"
                      repo (group-path group) artifact)))

(defn pom [repo group artifact version]
  (lazy-parse (format "%s/%s/%s/%s/%3$s-%4$s.pom"
                      repo (group-path group) artifact version)))

(defn versions [repo group artifact]
  (get-in-xml (metadata repo group artifact)
              [:metadata :versioning :versions :version/*]))

(defn dependencies [repo group artifact version]
  (map as-map
       (get-in-xml (pom repo group artifact version)
                   [:project :dependencies :dependency/*])))
