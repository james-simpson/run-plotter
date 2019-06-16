(ns run-plotter.views
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [reagent.core :as reagent]))

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
                                 js/L.Routing.control
                                 (.addTo map))]
           (reset! route-control-atom route-control)
           (.on map "click"
                (fn [e]
                  (re-frame/dispatch [:map-clicked e.latlng.lat e.latlng.lng])))
           (js/console.log route-control)))

       :component-did-update
       (fn [component]
         (let [new-waypoints (:waypoints (reagent/props component))]
           (print "leaflet-map-did-update" new-waypoints)
           (.setWaypoints @route-control-atom (clj->js new-waypoints))))})))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])
        waypoints (re-frame/subscribe [::subs/waypoints])]
    [:div
     [:h1 "Hello from " @name]
     [leaflet-map {:waypoints @waypoints}]
     ]))