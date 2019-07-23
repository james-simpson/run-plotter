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
  ::name
  (fn [db]
    (get-in db [:route :name])))

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
  ::route-time
  (fn [db]
    (let [{:keys [hours mins secs] :as route-time} (:route-time db)
          total-seconds (+ (* 3600 (or hours 0))
                           (* 60 (or mins 0))
                           (or secs 0))]
      (assoc route-time :total-seconds total-seconds))))

(re-frame/reg-sub
  ::save-in-progress?
  (fn [db _]
    (:save-in-progress? db)))

(re-frame/reg-sub
  ::saved-routes
  (fn [db]
    (:saved-routes db)))
