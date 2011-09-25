(ns depot.pom
  (:use [useful.string :only [camelize dasherize]])
  (:require [clojure.string :as s]))

(defn pomify [key]
  (->> key name camelize keyword))

(defmulti pom-sexps
  (fn [tag value] (keyword "depot.pom" (name tag))))

(defmethod pom-sexps :default
  ([tag value]
     (when value
       [(pomify tag) value])))

(defmethod pom-sexps ::list
  ([tag values]
     [(pomify tag) (map (partial pom-sexps
                                 (-> tag name (s/replace #"ies$" "y") keyword))
                        values)]))

(doseq [c [::dependencies ::repositories]]
  (derive c ::list))

(defmethod pom-sexps ::exclusions
  [tag values]
  [:exclusions
   (map
    (fn [dep]
      [:exclusion (map (partial apply pom-sexps)
                       {:group-id (namespace dep)
                        :artifact-id (name dep)})])
    values)])

(defmethod pom-sexps ::dependency
  ([_ [dep opts]]
     [:dependency
      (map (partial apply pom-sexps)
           {:group-id    (namespace dep)
            :artifact-id (name dep)
            :version     (:version opts)
            :classifier  (:classifier opts)
            :exclusions  (:exclusions opts)})]))

(defmethod pom-sexps ::repository
  ([_ [id url]]
     [:repository [:id id] [:url url]]))

(defmethod pom-sexps ::project
  ([tag values]
     (list
      [:project {:xmlns "http://maven.apache.org/POM/4.0.0"}
       [:modelVersion "4.0.0"]
       (map (partial apply pom-sexps)
            (select-keys values
                         (rseq
                          [:artifact-id :group-id :version :name
                           :description :license
                           :dependencies :repositories])))])))
