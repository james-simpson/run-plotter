(ns run-plotter.events
  (:require
   [re-frame.core :as re-frame]
   [run-plotter.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-fx
  :map-clicked
  (fn [{:keys [db]} [_ lat lng]]
    {:db (update db :waypoints #(concat % [[lat lng]]))}))

(re-frame/reg-event-fx
  :distance-updated
  (fn [{:keys [db]} [_ distance]]
    {:db (assoc db :total-distance distance)}))

(re-frame/reg-event-fx
  :clear-route
  (fn [{:keys [db]} _]
    {:db (assoc db :waypoints []
                   :deleted-waypoints []
                   :total-distance 0)}))

(re-frame/reg-event-fx
  :undo-waypoint
  (fn [{:keys [db]} _]
    (let [waypoint-to-remove (last (:waypoints db))]
      {:db (-> db
               (update :waypoints butlast)
               (update :deleted-waypoints #(concat % [waypoint-to-remove])))})))

(re-frame/reg-event-fx
  :redo-waypoint
  (fn [{:keys [db]} _]
    (let [waypoint-to-add (last (:deleted-waypoints db))]
      {:db (-> db
               (update :waypoints #(concat % [waypoint-to-add]))
               (update :deleted-waypoints butlast))})))

(re-frame/reg-event-fx
  :change-units
  (fn [{:keys [db]} [_ units]]
    {:db (assoc db :units units)}))
