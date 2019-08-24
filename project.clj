(defproject run-plotter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [compojure "1.6.1"]
                 [yogthos/config "1.1.2"]
                 [ring "1.7.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.13"]
                 [ragtime "0.8.0"]
                 [integrant "0.7.0"]
                 [aero "1.1.3"]
                 [duct/module.sql "0.5.0"]
                 [org.postgresql/postgresql "42.2.5"]
                 [com.layerware/hugsql "0.4.9"]
                 [honeysql "0.9.4"]]

  :main run-plotter.core

  :source-paths ["src/clj"]

  :profiles
  {:dev
   {:dependencies [[ring/ring-mock "0.4.0"]
                   [bigsy/pg-embedded-clj "0.0.8"]
                   [integrant/repl "0.3.1"]
                   [clj-http "3.9.1"]]}
   :uberjar {:main run-plotter.core
             :uberjar-name "run-plotter.jar"
             :aot [run-plotter.core]}})
