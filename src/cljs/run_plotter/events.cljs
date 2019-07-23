(ns run-plotter.events
  (:require
    [re-frame.core :as rf]
    [run-plotter.db :as db]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
    [day8.re-frame.undo :as undo]
    [day8.re-frame.http-fx]
    [ajax.core :as ajax]))

(def ^:private api-base-url "http://localhost:3000")

(rf/reg-event-fx
  ::initialize-db
  (fn-traced [_ _]
             {:db db/default-db
              :dispatch [:load-saved-routes]}))

(rf/reg-event-db
  :set-active-panel
  (fn [db [_ active-panel]]
    (assoc db :active-panel active-panel)))

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

(rf/reg-event-db
  :route-time-updated
  (fn [db [_ time-unit value]]
    (assoc-in db [:route-time time-unit] value)))

;;
;; ajax
;;

;; get routes
(rf/reg-event-fx
  :load-saved-routes
  (fn [_]
    {:http-xhrio {:method :get
                  :uri (str api-base-url "/routes")
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:get-routes-success]
                  :on-failure [:get-routes-failure]}}))

(rf/reg-event-db
  :get-routes-success
  (fn [db [_ routes]]
    (assoc db :saved-routes routes)))

(rf/reg-event-fx
  :get-routes-failure
  (fn [_]
    {:show-toast ["Unable to fetch saved routes" :failure]}))

;; post route
(rf/reg-event-fx
  :confirm-save
  (fn [{:keys [db]} _]
    {:db (assoc db :save-in-progress? false)
     :http-xhrio {:method :post
                  :uri (str api-base-url "/routes")
                  :params (:route db)
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:post-route-success]
                  :on-failure [:post-route-failure]}}))

(rf/reg-event-fx
  :post-route-success
  (fn [{:keys [db]} _]
    {:db db
     :dispatch [:load-saved-routes]
     :show-toast ["Route saved" :success]}))

(rf/reg-event-fx
  :post-route-failure
  (fn [_]
    {:show-toast ["Unable to save route" :failure]}))

;; delete route
(rf/reg-event-fx
  :delete-route
  (fn [{:keys [db]} [_ id]]
    {:http-xhrio {:method :delete
                  :uri (str api-base-url "/routes/" id)
                  :format (ajax/json-request-format)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:delete-route-success]
                  :on-failure [:delete-route-failure]}}))

(rf/reg-event-fx
  :delete-route-success
  (fn [{:keys [db]} _]
    {:db db
     :dispatch [:load-saved-routes]
     :show-toast ["Route deleted" :success]}))

(rf/reg-event-fx
  :delete-route-failure
  (fn [_]
    {:show-toast ["Unable to delete route" :failure]}))
