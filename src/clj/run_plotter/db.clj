(ns run-plotter.db
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "queries.sql")

(defn get-all-routes
  [db-spec]
  (sql-select-all-routes db-spec))

(defn get-route
  [db-spec id]
  (let [query-result (sql-select-route db-spec {:id id})
        route (-> query-result first (select-keys [:id :name :distance :polyline]))
        waypoints (->> query-result
                       (sort-by :waypoint_order)
                       (map (fn [{:keys [lat lng]}] [lat lng])))]
    (assoc route :waypoints waypoints)))

(defn insert-route!
  [db-spec {:keys [name distance polyline waypoints]}]
  (let [route-id (->> (sql-insert-route db-spec {:name name
                                                 :distance distance
                                                 :polyline polyline})
                      first
                      :id)
        waypoints-to-insert (map-indexed
                              (fn [index [lat lng]] [route-id index lat lng])
                              waypoints)]
    (sql-insert-waypoints db-spec {:waypoints waypoints-to-insert})
    route-id))

(defn delete-route!
  [db-spec id]
  (sql-delete-route! db-spec {:id id}))
