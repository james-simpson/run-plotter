(ns run-plotter.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::waypoints
  (fn [db]
    (:waypoints db)))

(re-frame/reg-sub
  ::total-distance
  (fn [db]
    (:total-distance db)))

(re-frame/reg-sub
  ::units
  (fn [db]
    (:units db)))

(re-frame/reg-sub
  ::offer-return-routes?
  (fn [{:keys [waypoints]}]
    (and (> (count waypoints) 1)
         (not= (first waypoints) (last waypoints)))))
