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
  [value-in-meters]
  (let [value-in-km (/ value-in-meters 1000)]
    [:h2 (gstring/format "%.3f km" value-in-km)]))

(defn- route-operations-panel
  [waypoints deleted-waypoints]
  [:div
   [:button
    {:on-click #(re-frame/dispatch [:clear-route])} "Clear route"]
   [:button
    {:on-click #(re-frame/dispatch [:undo-waypoint])
     :disabled (empty? waypoints)} "Undo"]
   [:button
    {:on-click #(re-frame/dispatch [:redo-waypoint])
     :disabled (empty? deleted-waypoints)} "Redo"]])

;;
;; main component
;;
(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])
        waypoints (re-frame/subscribe [::subs/waypoints])
        deleted-waypoints (re-frame/subscribe [::subs/deleted-waypoints])
        total-distance (re-frame/subscribe [::subs/total-distance])]
    [:div
     [:h1 "Hello from " @name]
     [leaflet-map {:waypoints @waypoints}]
     [distance @total-distance]
     [route-operations-panel @waypoints @deleted-waypoints]]))