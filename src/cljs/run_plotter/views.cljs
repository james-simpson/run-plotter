(ns run-plotter.views
  (:require
   [re-frame.core :as re-frame]
   [run-plotter.subs :as subs]
   ))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Hello from " @name]
     ]))
