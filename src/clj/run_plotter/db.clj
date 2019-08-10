(ns run-plotter.db
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "queries.sql")

(defn get-all-routes
  [db-spec]
  (sql-select-all-routes db-spec))

(defn get-route
  [db-spec id]
  (-> (sql-select-route db-spec {:id id})
      first
      (select-keys [:id :name :distance :polyline])))

(defn insert-route!
  [db-spec {:keys [name distance polyline]}]
  (let [route-id (->> (sql-insert-route db-spec {:name name
                                                 :distance distance
                                                 :polyline polyline})
                      first
                      :id)]
    route-id))

(defn delete-route!
  [db-spec id]
  (sql-delete-route! db-spec {:id id}))
