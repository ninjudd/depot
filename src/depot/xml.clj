(ns depot.xml)

(defn content [elt]
  (let [content (:content elt)]
    (if (or (next content) (map? (first content)))
      content
      (first content))))

(defn as-map [elt]
  (if (map? (first elt))
    (reduce (fn [acc e]
              (assoc acc (:tag e) (content e)))
            {} elt)
    elt))

(defn find-tags [elts tag]
  (map content
       (filter #(= tag (:tag %)) elts)))

(defn get-in-xml [elt [k & ks :as keys]]
  (when elt
    (cond (empty? keys)    elt
          (map? elt)       (when (= (:tag elt) k)
                             (recur (:content elt) ks))
          (= (name k) "*") (find-tags elt (keyword (namespace k)))
          :else            (recur (first (find-tags elt k)) ks))))
