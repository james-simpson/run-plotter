(ns run-plotter.db)

(def default-db
  {:active-panel :edit-route
   :waypoints []
   ; total route distance, in meters
   :total-distance 0
   :units :km})
