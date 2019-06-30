(ns run-plotter.db-client
  (:require
    [ragtime.jdbc :as jdbc]
    [ragtime.core :as ragtime]))

(defn ->db-specection
  [db-spec]
  (let [store (jdbc/sql-database db-spec)
        migrations (jdbc/load-resources "migrations")]
    (ragtime/migrate-all store {} migrations)))
