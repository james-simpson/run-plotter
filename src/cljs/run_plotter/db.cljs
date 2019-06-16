(ns run-plotter.db)

(def default-db
  {:name "re-frame"
   :waypoints []
   :deleted-waypoints []
   ; total route distance, in meters
   :total-distance 0})
