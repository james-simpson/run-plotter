(ns run-plotter.db)

(def default-db
  {:active-panel :edit-route
   :units :km

   ;; Saved routes panel
   :saved-routes []

   ;; Edit route panel
   :route {:waypoints []
           ; total route distance, in meters
           :distance 0}})
