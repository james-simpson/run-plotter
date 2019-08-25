(ns run-plotter.core
  (:require
    [run-plotter.config :as config]
    [integrant.core :as ig])
  (:gen-class))

(defn -main
  [& args]
  (let [conf (config/read-config)]
    (ig/load-namespaces conf)
    (ig/init conf)
    (println "Started app")))

