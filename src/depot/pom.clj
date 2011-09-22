(ns depot.pom
  (:use [useful.string :only [camelize dasherize]])
  (:require [clojure.string :as s]))

(defn pomify [key]
  (->> key name camelize keyword))

(defmulti xml-tags
  (fn [tag value] (keyword "depot.pom" (name tag))))

(defmethod xml-tags :default
  ([tag value]
     (when value
       [(pomify tag) value])))

(defmethod xml-tags ::list
  ([tag values]
     [(pomify tag) (map (partial xml-tags
                                 (-> tag name (s/replace #"ies$" "y") keyword))
                        values)]))

(doseq [c [::dependencies ::repositories]]
  (derive c ::list))

(defmethod xml-tags ::exclusions
  [tag values]
  [:exclusions
   (map
    (fn [dep]
      [:exclusion (map (partial apply xml-tags)
                       {:group-id (namespace dep)
                        :artifact-id (name dep)})])
    values)])

(defmethod xml-tags ::dependency
  ([_ [dep opts]]
     [:dependency
      (map (partial apply xml-tags)
           {:group-id    (namespace dep)
            :artifact-id (name dep)
            :version     (:version opts)
            :classifier  (:classifier opts)
            :exclusions  (:exclusions opts)})]))

(defmethod xml-tags ::repository
  ([_ [id url]]
     [:repository [:id id] [:url url]]))

(defmethod xml-tags ::project
  ([tag values]
     (list
      [:project {:xmlns "http://maven.apache.org/POM/4.0.0"}
       [:modelVersion "4.0.0"]
       (map (partial apply xml-tags)
            (select-keys values
                         (rseq
                          [:artifact-id :group-id :version :name
                           :description :license
                           :dependencies :repositories])))])))
