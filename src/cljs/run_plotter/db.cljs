(ns run-plotter.db)

(def default-db
  {:waypoints []
   :deleted-waypoints []
   ; total route distance, in meters
   :total-distance 0
   :units :km})
