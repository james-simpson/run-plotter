(ns run-plotter.views
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [reagent.core :as reagent]
    [goog.object]
    [goog.string :as gstring]))

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
        map (.setView (.map js/L "map") initial-lat-lng 17)
        _ (draw-tile-layer map)
        route-control (add-route-control map waypoints)]
    (reset! route-control-atom route-control)
    (.on map "click"
         (fn [e]
           (re-frame/dispatch [:map-clicked e.latlng.lat e.latlng.lng])))
    (.on route-control "routesfound"
         (fn [e]
           (let [total-distance (-> e .-routes first .-summary .-totalDistance)]
             (if (number? total-distance)
               (re-frame/dispatch [:distance-updated total-distance])))))))

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

(defn- distance
  [value-in-meters units]
  (let [value-in-km (/ value-in-meters 1000)
        value (if (= :miles units)
                (* value-in-km 0.621371)
                value-in-km)]
    [:h3.subtitle.is-3 {:style {:padding-top "1.5rem"}}
     (str (gstring/format "%.3f %s" value (name units)))]))

(defn- route-operations-panel
  [undos? redos? offer-return-routes?]
  [:div.button-panel
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
  [name value text checked]
  [:label.radio
   [:input {:type "radio" :value value :name name :checked checked}]
   text])

(defn- radio-buttons
  [{:keys [name selected-value on-change options]}]
  [:div.control {:on-change on-change}
   (map (fn [[value text]]
          (radio-input name value text (= value selected-value)))
        options)])

(defn- units-toggle
  [units]
  (radio-buttons {:name "units"
                  :selected-value units
                  :options [[:km "km"] [:miles "miles"]]
                  :on-change (fn [e]
                               (re-frame/dispatch [:change-units (keyword e.target.value)]))}))

(defn- navbar
  []
  [:nav.navbar.is-info {:style {:height "60px"}}
   [:div.navbar-brand
    [:a.navbar-item {:style {:padding-left "20px"}}
     [:img {:src "img/runner-icon.svg"
            :style {:max-height "2em"}}]]]
   [:div.navbar-menu
    [:div.navbar-start
     [:a.navbar-item "Create a route"]
     [:a.navbar-item "Saved routes"]]
    [:div.navbar-end
     [:a.navbar-item {:href "https://github.com/jsimpson-github/run-plotter"
                      :style {:margin-right "20px"}}
      [:img {:src "img/github-logo.svg"}]]]]])

;;
;; main component
;;
(defn main-panel []
  (let [waypoints (re-frame/subscribe [::subs/waypoints])
        ; the :undos? and :redos? subscriptions are added by the re-frame-undo
        ; library, along with the :undo and :redo event handlers
        undos? (re-frame/subscribe [:undos?])
        redos? (re-frame/subscribe [:redos?])
        offer-return-routes? (re-frame/subscribe [::subs/offer-return-routes?])
        total-distance (re-frame/subscribe [::subs/total-distance])
        units (re-frame/subscribe [::subs/units])]

    [:div
     [navbar]
     [:div {:style {:padding "25px"}}
      [units-toggle @units]
      [leaflet-map {:waypoints @waypoints}]
      [distance @total-distance @units]
      [route-operations-panel @undos? @redos? @offer-return-routes?]]]))