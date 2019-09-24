(ns run-plotter.db)

(def default-db
  {:active-panel :edit-route
   :units :km
   ; initial coords to centre on - roughly centred on the UK
   :centre [54.3 -3.23]
   :zoom 6

   ;; Saved routes panel
   :saved-routes []

   ;; Edit route panel
   :route {:waypoints []
           :co-ords []
           ; total route distance, in meters
           :distance 0}

   :save-in-progress? false
   :show-pace-calculator? false})
