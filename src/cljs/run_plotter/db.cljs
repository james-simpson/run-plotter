(ns run-plotter.db)

(def default-db
  {:active-panel :edit-route

   ;;
   ;; Saved routes panel
   ;;
   :saved-routes []

   ;;
   ;; Edit route panel
   ;;
   :waypoints []
   ; total route distance, in meters
   :total-distance 0
   :units :km})
