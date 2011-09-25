(ns depot.version
  (:use [clojure.string :only [join split]]))

(def version-levels [:major :minor :incremental :qualifier])

(defn version-map [version]
  (if-not (string? version)
    version
    (let [[version qualifier] (split version #"-" 2)]
      (into {:qualifier qualifier}
            (map vector
                 version-levels
                 (map #(Integer/parseInt %)
                      (split version #"\.")))))))

(defn version-mismatch? [expected-version actual-version]
  (let [expected (version-map expected-version)
        actual   (version-map actual-version)]
    (not (and (= (:major expected) (:major actual))
              (>= 0 (compare [(:minor expected) (:incremental expected)]
                             [(:minor actual)   (:incremental actual)]))))))

(defn newer? [a b]
  (let [a (version-map a)
        b (version-map b)]
    (< 0 (compare (vec (map a version-levels))
                  (vec (map b version-levels))))))

(defn snapshot? [version]
  (= "SNAPSHOT" (:qualifier (version-map version))))

(defn snapshot-timestamp []
  (let [time  (System/currentTimeMillis)
        ftime #(format (apply str (map (partial str "%1$t") %)) time)]
    (str (ftime "Ymd") "." (ftime "HMS"))))

(defn version-str [version]
  (let [version (version-map version)]
    (str (join "." (map #(get version % 0)
                        (butlast version-levels)))
         (when-let [qualifier (:qualifier version)]
           "-" (if (snapshot? version)
                 (snapshot-timestamp)
                 qualifier)))))