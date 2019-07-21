(ns run-plotter.views.edit-route
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [run-plotter.utils :as utils]
    [reagent.core :as reagent]
    [goog.object]
    [com.michaelgaare.clojure-polyline :as polyline]))

;;
;; map component
;;

; MAPBOX_TOKEN variable is loaded from resources/js/config.js
(def ^:private mapbox-token js/MAPBOX_TOKEN)

(defn- draw-tile-layer
  [map]
  (-> (js/L.tileLayer "http://{s}.tile.osm.org/{z}/{x}/{y}{r}.png"
                      (clj->js {:attribution "© OpenStreetMap contributors"}))
      (.addTo map)))

(defn- on-create-marker
  [waypoint-index waypoint total-waypoints]
  (let [start-waypoint? (= waypoint-index 0)
        last-waypoint? (= waypoint-index (dec total-waypoints))
        draw-marker? (or start-waypoint? last-waypoint?)
        marker-glyph (cond
                       start-waypoint? "A"
                       last-waypoint? "B")]
    (if draw-marker?
      (js/L.marker (.-latLng waypoint)
                   (clj->js {:icon (js/L.icon.glyph (clj->js {:glyph marker-glyph}))})))))

(defn- add-route-control
  [map waypoints]
  (let [router-options {:profile "mapbox/walking"
                        :routingOptions {:geometryOnly true
                                         :simplifyGeometry false}}]
    (-> (clj->js {:router (js/L.Routing.mapbox mapbox-token (clj->js router-options))
                  :waypoints waypoints
                  :waypointMode "snap"
                  :fitSelectedRoutes false
                  :createMarker on-create-marker})
        js/L.Routing.control
        (.addTo map))))

(defn- map-did-mount
  [route-control-atom component]
  (let [{:keys [waypoints]} (reagent/props component)
        initial-lat-lng #js [51.437382 -2.590950]
        leaflet-map (.setView (.map js/L "map") initial-lat-lng 17)
        _ (draw-tile-layer leaflet-map)
        route-control (add-route-control leaflet-map waypoints)]
    (reset! route-control-atom route-control)
    (.on leaflet-map "click"
         (fn [^js/mapClickEvent e]
           (re-frame/dispatch [:add-waypoint e.latlng.lat e.latlng.lng])))
    (.on route-control "routesfound"
         (fn [^js/lrmRoutesFoundEvent e]
           (let [^js/lrmRoute route (-> e .-routes first)
                 distance (-> route .-summary .-totalDistance)
                 encoded-polyline (->> (.-coordinates route)
                                       (map #(vector (.-lat %) (.-lng %)))
                                       polyline/encode)]
             (if (number? distance)
               (re-frame/dispatch [:route-updated {:distance distance
                                                   :polyline encoded-polyline}])))))))

(defn- map-did-update
  [route-control-atom component]
  (let [new-waypoints (:waypoints (reagent/props component))
        new-route-control (.setWaypoints @route-control-atom (clj->js new-waypoints))]
    (reset! route-control-atom new-route-control)))

(defn- leaflet-map []
  (let [route-control-atom (reagent/atom {})]
    (reagent/create-class
      {:reagent-render (fn [] [:div#map {:style {:height "500px"}}])
       :component-did-mount (partial map-did-mount route-control-atom)
       :component-did-update (partial map-did-update route-control-atom)})))

(defn- distance-panel
  [value-in-meters units]
  [:h3.subtitle.is-3 {:style {:padding-top "1.5rem"}}
   (utils/format-distance value-in-meters units 3)])

(defn- route-operations-panel
  [undos? redos? offer-return-routes?]
  [:div.button-panel
   [:button.button
    {:on-click #(re-frame/dispatch [:initiate-save])} "Save route"]
   [:button.button
    {:on-click #(re-frame/dispatch [:clear-route])} "Clear route"]
   [:button.button
    {:on-click #(re-frame/dispatch [:undo])
     :disabled (not undos?)} "Undo"]
   [:button.button
    {:on-click #(re-frame/dispatch [:redo])
     :disabled (not redos?)} "Redo"]
   [:button.button
    {:on-click #(re-frame/dispatch [:plot-shortest-return-route])
     :style {:margin-left "40px"}
     :disabled (not offer-return-routes?)}
    "Back to start"]
   [:button.button
    {:on-click #(re-frame/dispatch [:plot-same-route-back])
     :disabled (not offer-return-routes?)}
    "Same route back"]])

(defn- radio-input
  [name value selected-value text on-change]
  ^{:key value}
  [:label.radio
   [:input {:type "radio"
            :value value
            :name name
            :checked (= value selected-value)
            :on-change on-change}]
   text])

(defn- radio-buttons
  [{:keys [name selected-value on-change options]}]
  [:div.control {:on-change on-change}
   (for [[value text] options]
     (radio-input name value selected-value text on-change))])

(defn- units-toggle
  [units]
  (radio-buttons {:name "units"
                  :selected-value units
                  :options [[:km "km"] [:miles "miles"]]
                  :on-change (fn [e]
                               (re-frame/dispatch [:change-units (keyword e.target.value)]))}))

(defn- save-route-modal
  [show-save-form? route-name]
  (let [cancel-fn #(re-frame/dispatch [:cancel-save])
        confirm-fn #(re-frame/dispatch [:confirm-save])]
    [:div.modal {:style {:z-index 9999}
                 :class (if show-save-form? "is-active" "")}
     [:div.modal-background {:on-click cancel-fn}]
     [:div.modal-card
      [:header.modal-card-head
       [:p.modal-card-title "Save route"]
       [:button.delete {:aria-label "close"
                        :on-click cancel-fn}]]
      [:section.modal-card-body
       [:input#routeNameInput.input
        {:type "text"
         :placeholder "Route name"
         :style {:font-size "1.5em"}
         :value route-name
         :on-change (fn [e]
                      (re-frame/dispatch [:route-name-updated e.target.value]))}]]
      [:footer.modal-card-foot
       [:button.button.is-info {:on-click confirm-fn} "Save changes"]
       [:button.button {:on-click cancel-fn} "Cancel"]]]]))

(defn edit-route-panel []
  (let [waypoints (re-frame/subscribe [::subs/waypoints])
        ; the :undos? and :redos? subscriptions are added by the re-frame-undo
        ; library, along with the :undo and :redo event handlers
        undos? (re-frame/subscribe [:undos?])
        redos? (re-frame/subscribe [:redos?])
        offer-return-routes? (re-frame/subscribe [::subs/offer-return-routes?])
        distance (re-frame/subscribe [::subs/distance])
        route-name (re-frame/subscribe [::subs/name])
        units (re-frame/subscribe [::subs/units])
        save-in-progress? (re-frame/subscribe [::subs/save-in-progress?])]
    [:div
     [units-toggle @units]
     [leaflet-map {:waypoints @waypoints}]
     [distance-panel @distance @units]
     [route-operations-panel @undos? @redos? @offer-return-routes?]
     [save-route-modal @save-in-progress? @route-name]]))