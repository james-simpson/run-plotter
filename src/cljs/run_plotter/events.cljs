(ns run-plotter.events
  (:require
   [re-frame.core :as re-frame]
   [run-plotter.db :as db]
   [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
   [day8.re-frame.undo :as undo]))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-fx
  :map-clicked
  (undo/undoable "map click")
  (fn [{:keys [db]} [_ lat lng]]
    {:db (update db :waypoints #(concat % [[lat lng]]))}))

(re-frame/reg-event-fx
  :clear-route
  (undo/undoable "clear route")
  (fn [{:keys [db]} _]
    {:db (assoc db :waypoints []
                   :total-distance 0)}))

(re-frame/reg-event-fx
  :plot-shortest-return-route
  (undo/undoable "shortest return route")
  (fn [{:keys [db]} _]
    {:db (update db :waypoints #(concat % [(first %)]))}))

(re-frame/reg-event-fx
  :distance-updated
  (fn [{:keys [db]} [_ distance]]
    {:db (assoc db :total-distance distance)}))

(re-frame/reg-event-fx
  :change-units
  (fn [{:keys [db]} [_ units]]
    {:db (assoc db :units units)}))
