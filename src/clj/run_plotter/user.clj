(ns run-plotter.user
  (:require
    [run-plotter.config :as config]
    [integrant.core :as ig]
    [integrant.repl :as igr]
    [integrant.repl.state :as ig-state]
    [pg-embedded-clj.core :as pg-embedded]
    [ring.middleware.reload :refer [wrap-reload]]
    [run-plotter.db :as db]
    [duct.core.resource]))

(def figwheel-handler
  (let [handler-key :run-plotter.handler/handler
        conf (config/read-config)]
    (ig/load-namespaces conf [handler-key])
    (-> (ig/init conf [handler-key])
        (get handler-key)
        wrap-reload)))

(defn- start
  []
  (pg-embedded/init-pg {:port 54321})
  (let [conf (config/read-config)]
    (igr/set-prep! (constantly conf))
    (ig/load-namespaces conf)
    (igr/go)))

(defn- stop
  []
  (pg-embedded/halt-pg!)
  (igr/halt))

(comment
  (start)

  (def db-spec (get-in ig-state/system [:duct.database.sql/hikaricp :spec]))

  (def route-id (db/insert-route! db-spec "Bristol 10k" 10000 [[60.1 70.2]
                                                               [60.2 70.3]
                                                               [60.3 70.4]]))

  (db/get-route db-spec route-id)
  (db/get-all-routes db-spec)

  (stop))