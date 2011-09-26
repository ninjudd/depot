(ns depot.core
  (:use [depot.xml :only [get-in-xml as-map]]
        [depot.version :only [newer?]]
        [clojure.java.io :only [reader copy]]
        [clojure.data.xml :only [lazy-parse]]
        [useful.utils :only [queue]]
        [useful.map :only [map-keys index-by]])
  (:require [clojure.string :as s])
  (:import [java.io File]))

(defn group-path [group]
  (s/replace (name group) #"/." "/"))

(defn metadata [repo {:keys [group artifact]}]
  (try (lazy-parse (format "%s/%s/%s/maven-metadata.xml"
                           repo (group-path group) artifact))
       (catch Exception e)))

(def m2-repo (format "%s/.m2/repository/" (System/getProperty "user.home")))

(defn cached-uri [uri local]
  (let [local (File. local)]
    (when-not (.exists local)
      (.mkdirs (.getParentFile local))
      (.createNewFile local)
      (copy (reader uri) local))
    local))

(defn pom-path [repo {:keys [group artifact version]}]
  (format "%s/%s/%s/%s/%3$s-%4$s.pom"
          repo (group-path group) artifact version))

(defn pom [repo project]
  (try (lazy-parse (cached-uri (pom-path repo project)
                               (pom-path m2-repo project)))
       (catch Exception e)))

(defn versions [repo project]
  (get-in-xml (metadata repo project)
              [:metadata :versioning :versions :version/*]))

(defn project-key [project]
  (symbol (:group project) (:artifact project)))

(defn normalize-key [key]
  (keyword (s/replace (name key) #"Id$" "")))

(defn dependencies [repo project]
  (index-by project-key
            (for [dep (get-in-xml (pom repo project)
                                  [:project :dependencies :dependency/*])]
              (map-keys (as-map dep) normalize-key))))

(defn resolve-deps [repo project]
  (loop [projects   {}
         to-resolve (conj (queue) [(project-key project) project])]
    (if (empty? to-resolve)
      projects
      (let [[key project] (peek to-resolve)
            to-resolve    (pop  to-resolve)]
        (if-let [existing (get projects key)]
          (recur (if (newer? (:version project) (:version existing))
                   (assoc-in projects [key :version] (:version project))
                   projects)
                 to-resolve)
          (let [deps (dependencies repo project)]
            (recur (assoc projects key
                          (assoc project :dependencies deps))
                   (into to-resolve deps))))))))
