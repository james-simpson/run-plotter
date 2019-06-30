(ns run-plotter.views.saved-routes
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]))

(defn saved-routes-panel []
  (let [routes (re-frame/subscribe [::subs/saved-routes])]
    [:div
     [:h1 "Saved routes here"]
     (for [{:keys [id name distance]} @routes]
       ^{:key id}
       [:div (str "Route " id ", distance " distance)])]))