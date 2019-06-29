(ns run-plotter.config
  (:require
    [duct.core.resource]
    [aero.core :as aero]
    [clojure.java.io :as io]
    [integrant.core :as ig]))

(defmethod aero/reader 'ig/ref
  [_ _ value]
  (ig/ref value))

(defn read-config
  []
  (aero/read-config (io/resource "system.edn")))
