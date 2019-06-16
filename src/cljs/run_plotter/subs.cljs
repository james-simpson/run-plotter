(ns run-plotter.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::waypoints
  (fn [db]
    (:waypoints db)))

(re-frame/reg-sub
  ::deleted-waypoints
  (fn [db]
    (:deleted-waypoints db)))

(re-frame/reg-sub
  ::total-distance
  (fn [db]
    (:total-distance db)))

(re-frame/reg-sub
  ::units
  (fn [db]
    (:units db)))
