(ns depot.pom
  (:use [useful.string :only [camelize dasherize]])
  (:require [clojure.string :as s]))

(defn pomify [key]
  (->> key name camelize keyword))

(defmulti prxml-tags
  (fn [tag value] (keyword "depot.pom" (name tag))))

(defmethod prxml-tags :default
  ([tag value]
     (when value
       [(pomify tag) value])))

(defmethod prxml-tags ::list
  ([tag values]
     [(pomify tag) (map (partial prxml-tags
                                 (-> tag name (s/replace #"ies$" "y") keyword))
                        values)]))

(doseq [c [::dependencies ::repositories]]
  (derive c ::list))

(defmethod prxml-tags ::exclusions
  [tag values]
  [:exclusions
   (map
    (fn [dep]
      [:exclusion (map (partial apply prxml-tags)
                       {:group-id (namespace dep)
                        :artifact-id (name dep)})])
    values)])

(defmethod prxml-tags ::dependency
  ([_ [dep opts]]
     [:dependency
      (map (partial apply prxml-tags)
           {:group-id    (namespace dep)
            :artifact-id (name dep)
            :version     (:version opts)
            :classifier  (:classifier opts)
            :exclusions  (:exclusions opts)})]))

(defmethod prxml-tags ::repository
  ([_ [id url]]
     [:repository [:id id] [:url url]]))

(defmethod prxml-tags ::project
  ([tag values]
     (list
      [:decl!]
      [:project {:xmlns "http://maven.apache.org/POM/4.0.0"}
       [:modelVersion "4.0.0"]
       (map (partial apply prxml-tags)
            (select-keys values
                         (rseq
                          [:artifact-id :group-id :version :name
                           :description :license
                           :dependencies :repositories])))])))

