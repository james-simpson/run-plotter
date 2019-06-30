(ns run-plotter.core
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [run-plotter.events :as events]
   [run-plotter.views.base :refer [base-view]]
   [run-plotter.config :as config]
   [run-plotter.routes :as routes]
   ))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [base-view]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (routes/listen-for-url-changes!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch [::events/load-saved-routes])
  (dev-setup)
  (mount-root))
