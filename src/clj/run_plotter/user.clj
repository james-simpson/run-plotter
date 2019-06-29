(ns run-plotter.user
  (:require
    [run-plotter.config :as config]
    [integrant.core :as ig]
    [integrant.repl :as igr]
    [integrant.repl.state :as ig-state]
    [pg-embedded-clj.core :as pg-embedded]
    [run-plotter.db :as db]
    [duct.core.resource]))

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

  (def db-conn (:spec (:duct.database.sql/hikaricp ig-state/system)))

  (def route-id (db/insert-route! db-conn "Bristol 10k" 10000 [[60.1 70.2]
                                                               [60.2 70.3]
                                                               [60.3 70.4]]))

  (db/get-route db-conn route-id)
  (db/get-all-routes db-conn)

  (stop))