(ns run-plotter.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
  ::waypoints
  (fn [db]
    (:waypoints db)))

(re-frame/reg-sub
  ::total-distance
  (fn [db]
    (:total-distance db)))
