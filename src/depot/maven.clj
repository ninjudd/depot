(ns depot.maven
  (:use uncle.core depot.deps)
  (:import [org.apache.maven.artifact.ant DependenciesTask RemoteRepository WritePomTask InstallTask Pom]
           [org.apache.tools.ant.taskdefs Delete]
           [org.apache.maven.model Dependency Exclusion]
           [java.io File]))

(defn- add-repositories [task repositories]
  (doseq [[id url] repositories]
    (.addConfiguredRemoteRepository task
      (ant-type RemoteRepository {:id id :url url}))))

(defn- exclusion [dep]
  (ant-type Exclusion {:group-id (namespace dep) :artifact-id (name dep)}))

(defn- dependencies [spec type]
  (for [[dep opts] (get spec type)]
    (ant-type Dependency
      {:group-id    (namespace dep)
       :artifact-id (name dep)
       :version     (or (:version opts) (get-in spec [:dependencies dep :version]) "LATEST")
       :classifier  (:classifier opts)
       :exclusions  (map exclusion (concat *exclusions* (:exclusions opts)))})))

(defn- add-dependencies [task deps]
  (doseq [dep deps]
    (.addDependency task dep)))

(defmethod fetch-deps :maven [spec type]
  (when-let [deps (seq (dependencies spec type))]
    (ant DependenciesTask {:fileset-id "depot.fileset" :path-id (:name spec)}
      (add-repositories (into *repositories* (:repositories spec)))
      (add-dependencies deps))
    (fileset-seq (get-reference "depot.fileset"))))

(defmethod clear-deps :maven [spec type]
  (doseq [dep (dependencies spec type)]
    (let [path (.getSystemPath dep)]
      (ant Delete {:file path}))))

(defmethod publish :maven [spec jar pom]
  (ant Pom {:file pom :id "depot.pom"})
  (ant InstallTask {:file jar :pom-ref-id "depot.pom"}))
