(ns run-plotter.db
  (:require [hugsql.core :as hugsql]))

(hugsql/def-db-fns "queries.sql")

(defn get-all-routes
  [db-conn]
  (sql-select-all-routes db-conn))

(defn get-route
  [db-conn id]
  (let [query-result (sql-select-route db-conn {:id id})
        route (-> query-result first (select-keys [:id :name :distance]))
        waypoints (->> query-result
                       (sort-by :waypoint_order)
                       (map (fn [{:keys [lat lng]}] [lat lng])))]
    (assoc route :waypoints waypoints)))

(defn insert-route!
  [db-conn name distance waypoints]
  (let [route-id (->> (sql-insert-route db-conn {:name name
                                                 :distance distance})
                      first
                      :id)
        waypoints-to-insert (map-indexed
                              (fn [index [lat lng]] [route-id index lat lng])
                              waypoints)]
    (sql-insert-waypoints db-conn {:waypoints waypoints-to-insert})
    route-id))


