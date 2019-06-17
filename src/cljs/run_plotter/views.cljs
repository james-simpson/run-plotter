(ns run-plotter.views
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [reagent.core :as reagent]
    [goog.object]
    [goog.string :as gstring]
    [antizer.reagent :as ant]))

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

(defn- add-route-control
  [map waypoints]
  (let [router-options {:profile "mapbox/walking"
                        :routingOptions {:geometryOnly true
                                         :simplifyGeometry false}}]
    (-> (clj->js {:router (js/L.Routing.mapbox mapbox-token (clj->js router-options))
                  :waypoints waypoints
                  :waypointMode "snap"
                  :fitSelectedRoutes false})
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
    [:h2 {:style {:padding-top "10px"}}
     (str (gstring/format "%.3f %s" value (name units)))]))

(defn- route-operations-panel
  [undos? redos?]
  [:div
   [ant/button
    {:on-click #(re-frame/dispatch [:clear-route])} "Clear route"]
   [ant/button
    {:on-click #(re-frame/dispatch [:undo])
     :disabled (not undos?)} "Undo"]
   [ant/button
    {:on-click #(re-frame/dispatch [:redo])
     :disabled (not redos?)} "Redo"]])

(defn- advanced-route-operations-panel
  []
  [:div
   [ant/button
    {:on-click #(re-frame/dispatch [:plot-shortest-return-route])}
    "Back to start"]
   [ant/button
    {:on-click #(re-frame/dispatch [:plot-same-route-back])}
    "Same route back"]])

(defn- units-toggle
  [units]
  [ant/radio-group {:value units
                    :style {:padding-bottom "10px"}
                    :on-change (fn [e]
                                 (re-frame/dispatch [:change-units (keyword e.target.value)]))}
   [ant/radio {:value :km} "km"]
   [ant/radio {:value :miles} "miles"]])

;;
;; main component
;;
(defn main-panel []
  (let [waypoints (re-frame/subscribe [::subs/waypoints])
        ; the :undos? and :redos? subscriptions are added by the re-frame-undo
        ; library, along with the :undo and :redo event handlers
        undos? (re-frame/subscribe [:undos?])
        redos? (re-frame/subscribe [:redos?])
        total-distance (re-frame/subscribe [::subs/total-distance])
        units (re-frame/subscribe [::subs/units])]
    [:div {:style {:padding "25px"}}
     [:h1 "Plot a run"]
     [units-toggle @units]
     [leaflet-map {:waypoints @waypoints}]
     [distance @total-distance @units]
     [route-operations-panel @undos? @redos?]
     [advanced-route-operations-panel]]))