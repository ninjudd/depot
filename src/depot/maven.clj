(ns depot.maven
  (:use uncle.core depot.deps
        [useful.map :only [filter-vals]])
  (:import [org.apache.maven.artifact.ant DependenciesTask RemoteRepository InstallTask Pom]
           [org.apache.tools.ant.taskdefs Delete]
           [org.apache.maven.model Dependency Exclusion]
           [java.io File]))

(defn- add-repositories [task repositories]
  (doseq [[id url] repositories]
    (.addConfiguredRemoteRepository task
      (ant-type RemoteRepository {:id id :url url}))))

(defn- exclusion [dep]
  (ant-type Exclusion {:group-id (namespace dep) :artifact-id (name dep)}))

(defn- dependencies [deps]
  (for [[dep opts] deps]
    (ant-type Dependency
      {:group-id    (namespace dep)
       :artifact-id (name dep)
       :version     (or (:version opts) "LATEST")
       :classifier  (:classifier opts)
       :exclusions  (map exclusion (concat *exclusions* (:exclusions opts)))})))

(defn- add-dependencies [task deps]
  (doseq [dep deps]
    (.addDependency task dep)))

(defmethod fetch-deps :maven [spec & [f]]
  (when-let [deps (seq (dependencies
                        (if f
                          (filter-vals (:dependencies spec) f)
                          (:dependencies spec))))]
    (ant DependenciesTask {:fileset-id "depot.fileset" :path-id (:name spec)}
      (add-repositories (into *repositories* (:repositories spec)))
      (add-dependencies deps))
    (fileset-seq (get-reference "depot.fileset"))))

(defmethod publish :maven [spec jar pom]
  (ant Pom {:file pom :id "depot.pom"})
  (ant InstallTask {:file jar :pom-ref-id "depot.pom"}))
