(ns run-plotter.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.db :as db]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
    [day8.re-frame.undo :as undo]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]))

(re-frame/reg-event-db
  ::initialize-db
  (fn-traced [_ _]
             db/default-db))

(re-frame/reg-event-db
  :set-active-panel
  (fn [db [_ active-panel]]
    (assoc db :active-panel active-panel)))

(re-frame/reg-event-fx
  :add-waypoint
  (undo/undoable "add waypoint")
  (fn [{:keys [db]} [_ lat lng]]
    {:db (update-in db [:route :waypoints] #(concat % [[lat lng]]))}))

; todo - make this configurable
(def ^:private api-base-url "http://localhost:3449")

(defn- get-routes
  []
  (go (let [response (<! (http/get (str api-base-url "/routes")
                                   {:as :json}))
            routes (:body response)]
        (print  "GET response" response)
        (re-frame/dispatch [:set-saved-routes routes]))))

(defn- post-route!
  [route]
  (go (let [response (<! (http/post (str api-base-url "/routes")
                                    {:json-params route}))]
        (prn "Response to post:" (:body response)))))

(defn- delete-route!
  [id]
  (go (<! (http/delete (str api-base-url "/routes/" id)))))

(re-frame/reg-event-fx
  ::load-saved-routes
  (fn [{:keys [db]} _]
    (get-routes)
    {:db db}))

(re-frame/reg-event-fx
  :set-saved-routes
  (fn [{:keys [db]} [_ routes]]
    {:db (assoc db :saved-routes routes)}))

(re-frame/reg-event-fx
  :save-route
  (fn [{:keys [db]} _]
    (post-route! (:route db))
    (re-frame/dispatch [::load-saved-routes])
    {:db db}))

(re-frame/reg-event-fx
  :delete-route
  (fn [{:keys [db]} [_ id]]
    (delete-route! id)
    (re-frame/dispatch [::load-saved-routes])
    {:db db}))

(re-frame/reg-event-fx
  :clear-route
  (undo/undoable "clear route")
  (fn [{:keys [db]} _]
    {:db (assoc db :route {:waypoints []
                           :distance 0})}))

(re-frame/reg-event-fx
  :plot-shortest-return-route
  (undo/undoable "shortest return route")
  (fn [{:keys [db]} _]
    {:db (update-in db [:route :waypoints] #(concat % [(first %)]))}))

(re-frame/reg-event-fx
  :plot-same-route-back
  (undo/undoable "same route back")
  (fn [{:keys [db]} _]
    (let [return-waypoints (->> (:route db)
                                :waypoints
                                butlast
                                reverse)]
      {:db (update-in db [:route :waypoints]
                      #(concat % return-waypoints))})))

(re-frame/reg-event-fx
  :distance-updated
  (fn [{:keys [db]} [_ distance]]
    {:db (assoc-in db [:route :distance] distance)}))

(re-frame/reg-event-fx
  :change-units
  (fn [{:keys [db]} [_ units]]
    {:db (assoc db :units units)}))
