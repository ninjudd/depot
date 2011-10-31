(defproject depot "0.1.8"
  :description "Library for fetching Clojure dependencies."
  :dependencies [[clojure "1.2.0"]
                 [uncle "0.2.3"]
                 [useful "0.7.0-alpha3"]
                 [org.clojars.ninjudd/maven-ant-tasks "2.1.0" :exclusions [ant/ant]]])
