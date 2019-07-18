(ns run-plotter.subs
  (:require
    [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::active-panel
  (fn [db _]
    (:active-panel db)))

(re-frame/reg-sub
  ::waypoints
  (fn [db]
    (get-in db [:route :waypoints])))

(re-frame/reg-sub
  ::distance
  (fn [db]
    (get-in db [:route :distance])))

(re-frame/reg-sub
  ::units
  (fn [db]
    (:units db)))

(re-frame/reg-sub
  ::offer-return-routes?
  (fn [{{:keys [waypoints]} :route}]
    (and (> (count waypoints) 1)
         (not= (first waypoints) (last waypoints)))))

(re-frame/reg-sub
  ::saved-routes
  (fn [db]
    (:saved-routes db)))
