(ns run-plotter.server
  (:require
    [ring.adapter.jetty :as jetty]
    [integrant.core :as ig]))

(defmethod ig/init-key ::server
  [_ {:keys [handler port]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! ::server
  [_ server]
  (.stop server))
