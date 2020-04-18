(ns run-plotter.events
  (:require
    [re-frame.core :as rf]
    [run-plotter.db :as db]
    [run-plotter.utils :as utils]
    [day8.re-frame.tracing :refer-macros [fn-traced defn-traced]]
    [day8.re-frame.undo :as undo]
    [day8.re-frame.http-fx]
    [ajax.core :as ajax]
    [com.michaelgaare.clojure-polyline :as polyline]
    [clojure.string :as str]
    ["bulma-toast" :as bulma-toast]))

(goog-define API_BASE_URL "")

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
    (bulma-toast/toast #js {:message message
                            :position "top-center"
                            :type (case type
                                    :success "is-success"
                                    :failure "is-danger")})))

(rf/reg-event-db
  :set-map-obj
  (fn [db [_ map-obj]]
    (assoc db :map-obj map-obj)))

(defn- co-ords->map-bounds
  [co-ords]
  (let [lats (map first co-ords)
        lngs (map second co-ords)
        min-lat (apply min lats)
        max-lat (apply max lats)
        min-lng (apply min lngs)
        max-lng (apply max lngs)]
    [[min-lat min-lng] [max-lat max-lng]]))

(rf/reg-fx
  :centre-on-users-location
  (fn [map-obj]
    (js/navigator.geolocation.getCurrentPosition
      (fn [position]
        (let [co-ords (.-coords position)
              lat (.-latitude co-ords)
              lng (.-longitude co-ords)
              zoom 16]
          (rf/dispatch [:set-location [lat lng]])
          (.setView map-obj #js [lat lng] zoom))))))

(rf/reg-fx
  :pan-map
  (fn [[map-obj co-ords]]
    (.panTo map-obj (clj->js co-ords))))

(rf/reg-fx
  :fit-map-to-bounds
  (fn [[map-obj co-ords]]
    (.fitBounds map-obj (clj->js co-ords))))

(rf/reg-event-fx
  :centre-map
  (fn [{:keys [db]} _]
    (if (get-in db [:route :id])
      (let [co-ords (get-in db [:route :co-ords])]
        {:fit-map-to-bounds [(:map-obj db) (co-ords->map-bounds co-ords)]})
      {:centre-on-users-location (:map-obj db)})))

; TODO - move to server side and regenerate token
(def mapbox-token "pk.eyJ1IjoianNpbXBzb245MiIsImEiOiJjandzY2ExZDIwbTB3NDRwNWFlZzYyenRvIn0.Vp-UX6Hs7efpjiERiVMVZQ")

(rf/reg-event-fx
  ::set-route
  (fn [{:keys [db]} [_ co-ords distance]]
    {:db (update db :route #(assoc % :co-ords co-ords
                                     :distance distance))
     :pan-map [(:map-obj db) (last co-ords)]}))

(rf/reg-event-fx
  :add-waypoint
  (undo/undoable "add waypoint")
  (fn [{:keys [db]} [_ lat lng]]
    (let [route-co-ords (-> db :route :co-ords)]
      (cond
        (empty? route-co-ords)
        {:dispatch [::set-route [[lat lng]] 0]}

        (not (:snap-to-paths? db))
        (let [new-distance (+ (get-in db [:route :distance])
                              (utils/distance-between-lat-lngs [lat lng] (last route-co-ords)))]
          {:dispatch [::set-route
                      (concat route-co-ords [[lat lng]])
                      new-distance]})

        :else
        (let [co-ord-string (->> [(last route-co-ords) [lat lng]]
                                 (map (fn [[lat lng]] (str lng "," lat)))
                                 (str/join ";"))]
          {:http-xhrio {:method :get
                        :uri (str "https://api.mapbox.com/directions/v5/mapbox/walking/" co-ord-string
                                  "?access_token=" mapbox-token)
                        :format (ajax/json-request-format)
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success [:routing-success]
                        :on-failure [:routing-failure]}})))))

(rf/reg-event-fx
  :routing-success
  (fn [{:keys [db]} [_ response]]
    (let [route (first (:routes response))
          co-ords (polyline/decode (:geometry route))
          prev-co-ords (get-in db [:route :co-ords])
          ; If the previous co-ords were just a single point, then
          ; discard that and use the routing results as we want
          ; all co-ords to be snapped to paths.
          new-co-ords (if (= (count prev-co-ords) 1)
                        co-ords
                        (concat prev-co-ords co-ords))
          new-distance (+ (get-in db [:route :distance])
                          (:distance route))]
      {:dispatch [::set-route new-co-ords new-distance]
       :pan-map [(:map-obj db) (last new-co-ords)]})))

(rf/reg-event-fx
  :routing-failure
  (fn [_]
    {:show-toast ["Unable to find route" :failure]}))

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
    (assoc db :route {:co-ords []
                      :distance 0})))

(rf/reg-event-fx
  :plot-shortest-return-route
  (fn [{:keys [db]} _]
    (let [[lat lng] (first (get-in db [:route :co-ords]))]
      {:db db
       :dispatch [:add-waypoint lat lng]})))

(rf/reg-event-fx
  :plot-same-route-back
  (undo/undoable "same route back")
  (fn [{:keys [db]} _]
    (let [co-ords (get-in db [:route :co-ords])
          return-co-ords (reverse (butlast co-ords))
          new-distance (* 2 (get-in db [:route :distance]))]
      {:dispatch [::set-route (concat co-ords return-co-ords) new-distance]})))

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

(rf/reg-event-db
  :set-location
  (fn [db [_ [lat lng]]]
    (assoc db :device-location [lat lng]
              :zoom 16)))

(rf/reg-event-db
  :open-pace-calculator
  (fn [db]
    (assoc db :show-pace-calculator? true)))

(rf/reg-event-db
  :close-pace-calculator
  (fn [db]
    (assoc db :show-pace-calculator? false)))

(rf/reg-event-db
  :set-snap-to-paths
  (fn [db [_ snap-to-paths?]]
    (assoc db :snap-to-paths? snap-to-paths?)))

;;
;; ajax
;;

;; get routes
(rf/reg-event-fx
  :load-saved-routes
  (fn [_]
    {:http-xhrio {:method :get
                  :uri (str API_BASE_URL "/routes")
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

;; get saved route
(rf/reg-event-fx
  :load-route
  (fn [_ [_ id]]
    {:http-xhrio {:method :get
                  :uri (str API_BASE_URL "/routes/" id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [:get-route-success]
                  :on-failure [:get-route-failure]}}))

(rf/reg-event-fx
  :get-route-success
  (fn [{:keys [db]} [_ route]]
    (let [polyline (:polyline route)
          co-ords (polyline/decode polyline)]
      {:db (assoc db :route (-> route
                                (dissoc :polyline)
                                (assoc :co-ords co-ords)))
       :dispatch [:set-active-panel :edit-route]})))

(rf/reg-event-fx
  :get-route-failure
  (fn [_]
    {:show-toast ["Unable to fetch route" :failure]}))

;; post route
(rf/reg-event-fx
  :confirm-save
  (fn [{:keys [db]} _]
    (let [route (:route db)
          polyline (polyline/encode (:co-ords route))]
      {:db (assoc db :save-in-progress? false)
       :http-xhrio {:method :post
                    :uri (str API_BASE_URL "/routes")
                    :params (assoc route :polyline polyline)
                    :format (ajax/json-request-format)
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:post-route-success]
                    :on-failure [:post-route-failure]}})))

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
  (fn [_ [_ id]]
    {:http-xhrio {:method :delete
                  :uri (str API_BASE_URL "/routes/" id)
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
