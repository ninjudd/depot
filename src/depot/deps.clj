(ns depot.deps)

(def *exclusions*   nil)
(def *repositories* nil)

(defn- backend [spec & args]
  (or (:depot spec) :maven))

(defmulti fetch-deps
  "Fetch dependencies into the local cache and return a sequence of the jars."
  backend)

(defmulti publish
  "Publish a project jar to the local cache."
  backend)
