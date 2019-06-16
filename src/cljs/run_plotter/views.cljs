(ns run-plotter.views
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [reagent.core :as reagent]
    [goog.object]
    [goog.string :as gstring]))

; leaflet component
(def ^:private mapbox-token js/MAPBOX_TOKEN)

(def ^:private router-options {:profile "mapbox/walking"
                               :routingOptions {:geometryOnly true
                                                :simplifyGeometry false}})

(defn leaflet-map [{:keys [waypoints]}]
  (let [route-control-atom (reagent/atom {})]
    (reagent/create-class
      {:reagent-render (fn [] [:div#map {:style {:height "360px"}}])

       :component-did-mount
       (fn leaflet-map-did-mount [component]
         (let [initial-lat-lng #js [51.437382 -2.590950]
               map (.setView (.map js/L "map") initial-lat-lng 17)
               _ (-> (.tileLayer js/L "http://{s}.tile.osm.org/{z}/{x}/{y}{r}.png"
                                 (clj->js {:attribution "© OpenStreetMap contributors"}))
                     (.addTo map))
               route-control (-> (clj->js {:router (js/L.Routing.mapbox mapbox-token (clj->js router-options))
                                           :waypoints waypoints
                                           :waypointMode "snap"
                                           :fitSelectedRoutes false})
                                 js/L.Routing.control)
               _ (.addTo route-control map)]
           (reset! route-control-atom route-control)
           (.on map "click"
                (fn [e]
                  (re-frame/dispatch [:map-clicked e.latlng.lat e.latlng.lng])))
           (.on route-control "routesfound"
                (fn [e]
                  (let [total-distance (.-totalDistance (.-summary (first e.routes)))]
                    (if (number? total-distance)
                      (re-frame/dispatch [:distance-updated total-distance])))))
           (js/console.log "control" @route-control-atom)))

       :component-did-update
       (fn [component]
         (print "leaflet-map-did-update")
         (let [new-waypoints (:waypoints (reagent/props component))
               new-route-control (.setWaypoints @route-control-atom (clj->js new-waypoints))]
           (js/console.log "updated control" new-route-control)
           (reset! route-control-atom new-route-control)))})))

(defn- distance
  [value-in-meters]
  (let [value-in-km (/ value-in-meters 1000)]
    [:h2 (gstring/format "%.3f km" value-in-km)]))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])
        waypoints (re-frame/subscribe [::subs/waypoints])
        total-distance (re-frame/subscribe [::subs/total-distance])]
    [:div
     [:h1 "Hello from " @name]
     [leaflet-map {:waypoints @waypoints}]
     [distance @total-distance]
     ]))