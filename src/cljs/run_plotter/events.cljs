(ns run-plotter.events
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [re-frame.core :as rf]
    [run-plotter.db :as db]
    [run-plotter.utils :as utils]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
    [day8.re-frame.undo :as undo]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]))

(def ^:private api-base-url "http://localhost:3000")

(rf/reg-event-fx
  ::initialize-db
  (fn-traced [_ _]
             {:db db/default-db
              :load-saved-routes []}))

(defn- get-routes
  []
  (go (let [response (<! (http/get (str api-base-url "/routes")
                                   {:as :json
                                    :with-credentials? false}))
            routes (:body response)]
        (rf/dispatch [:set-saved-routes routes]))))

(rf/reg-event-db
  :set-active-panel
  (fn [db [_ active-panel]]
    (assoc db :active-panel active-panel)))

(rf/reg-fx :load-saved-routes get-routes)

(rf/reg-fx
  :ajax
  (fn [{:keys [db]} [_ routes]]
    {:db (assoc db :saved-routes routes)}))

(rf/reg-fx
  :show-toast
  (fn [[message type]]
    (js/bulmaToast.toast #js {:message message
                              :position "top-center"
                              :type (case type
                                      :success "is-success"
                                      :failure "is-danger")})))

(rf/reg-event-db
  :add-waypoint
  (undo/undoable "add waypoint")
  (fn [db [_ lat lng]]
    (update-in db [:route :waypoints] #(concat % [[lat lng]]))))

(defn- post-route!
  [route]
  (go (let [response (<! (http/post (str api-base-url "/routes")
                                    {:json-params route
                                     :with-credentials? false}))]
        (if (:success response)
          (rf/dispatch [:save-successful])
          (utils/display-toast "Unable to save route" :failure)))))

(defn- delete-route!
  [id]
  (go (let [response (<! (http/delete (str api-base-url "/routes/" id)
                                      {:with-credentials? false}))]
        (if (:success response)
          (rf/dispatch [:delete-successful])
          (utils/display-toast "Unable to delete route" :failure)))))

(rf/reg-event-fx
  :set-saved-routes
  (fn [{:keys [db]} [_ routes]]
    {:db (assoc db :saved-routes routes)}))

(rf/reg-event-db
  :initiate-save
  (fn [db _]
    (-> db
        (assoc :save-in-progress? true)
        (update :route #(dissoc % :name)))))

(rf/reg-event-db
  :cancel-save
  #(assoc % :save-in-progress? false))

(rf/reg-event-fx
  :confirm-save
  (fn [{:keys [db]} _]
    (post-route! (:route db))
    {:db (assoc db :save-in-progress? false)
     :load-saved-routes []}))

(rf/reg-event-fx
  :save-successful
  (fn [{:keys [db]} _]
    {:db db
     :load-saved-routes []
     :show-toast ["Route saved" :success]}))

(rf/reg-event-fx
  :delete-route
  (fn [{:keys [db]} [_ id]]
    (delete-route! id)
    {:db db}))

(rf/reg-event-fx
  :delete-successful
  (fn [{:keys [db]} _]
    {:db db
     :load-saved-routes []
     :show-toast ["Route deleted" :success]}))

(rf/reg-event-db
  :clear-route
  (undo/undoable "clear route")
  (fn [db _]
    (assoc db :route {:waypoints []
                      :distance 0})))

(rf/reg-event-db
  :plot-shortest-return-route
  (undo/undoable "shortest return route")
  (fn [db _]
    (update-in db [:route :waypoints] #(concat % [(first %)]))))

(rf/reg-event-db
  :plot-same-route-back
  (undo/undoable "same route back")
  (fn [db _]
    (let [return-waypoints (->> (:route db)
                                :waypoints
                                butlast
                                reverse)]
      (update-in db [:route :waypoints]
                 #(concat % return-waypoints)))))

(rf/reg-event-db
  :route-updated
  (fn [db [_ {:keys [distance polyline]}]]
    (update db :route #(assoc % :distance distance
                                :polyline polyline))))

(rf/reg-event-db
  :route-name-updated
  (fn [db [_ name]]
    (assoc-in db [:route :name] name)))

(rf/reg-event-db
  :change-units
  (fn [db [_ units]]
    (assoc db :units units)))
