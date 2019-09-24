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
  ::co-ords
  (fn [db]
    (get-in db [:route :co-ords])))

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
  (fn [db] (:units db)))

(re-frame/reg-sub
  ::centre
  (fn [db] (:centre db)))

(re-frame/reg-sub
  ::device-location
  (fn [db] (:device-location db)))

(re-frame/reg-sub
  ::zoom
  (fn [db] (:zoom db)))

(re-frame/reg-sub
  ::offer-return-routes?
  (fn [{{:keys [co-ords]} :route}]
    (and (> (count co-ords) 1)
         (not= (first co-ords) (last co-ords)))))

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
    (->> (:saved-routes db)
         (sort-by :distance))))
