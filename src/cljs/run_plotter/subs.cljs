(ns run-plotter.subs
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub ::active-panel :active-panel)

(rf/reg-sub ::waypoints (fn [db]
                          (get-in db [:route :waypoints])))

(rf/reg-sub ::co-ords (fn [db]
                        (get-in db [:route :co-ords])))

(rf/reg-sub ::name (fn [db]
                     (get-in db [:route :name])))

(rf/reg-sub ::distance (fn [db]
                         (get-in db [:route :distance])))

(rf/reg-sub ::units :units)

(rf/reg-sub ::centre :centre)

(rf/reg-sub ::device-location :device-location)

(rf/reg-sub ::zoom :zoom)

(rf/reg-sub
  ::offer-return-routes?
  (fn [{{:keys [co-ords]} :route}]
    (and (> (count co-ords) 1)
         (not= (first co-ords) (last co-ords)))))

(rf/reg-sub
  ::route-time
  (fn [db]
    (let [{:keys [hours mins secs] :as route-time} (:route-time db)
          total-seconds (+ (* 3600 (or hours 0))
                           (* 60 (or mins 0))
                           (or secs 0))]
      (assoc route-time :total-seconds total-seconds))))

(rf/reg-sub
  ::save-in-progress?
  (fn [db _]
    (:save-in-progress? db)))

(rf/reg-sub ::show-pace-calculator? :show-pace-calculator?)

(rf/reg-sub
  ::saved-routes
  (fn [db]
    (->> (:saved-routes db)
         (sort-by :distance))))
