(ns run-plotter.views.saved-routes
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [reagent.core :as reagent]
    [com.michaelgaare.clojure-polyline :as polyline]))

(defn- draw-tile-layer
  [map]
  (-> (js/L.tileLayer "http://{s}.tile.osm.org/{z}/{x}/{y}{r}.png"
                      (clj->js {:attribution "© OpenStreetMap contributors"}))
      (.addTo map)))

(defn- route->leaflet-polyline
  [{:keys [polyline]}]
  (js/L.polyline (clj->js (polyline/decode polyline))))

(defn- draw-routes
  [routes leaflet-map]
  (let [polylines (map route->leaflet-polyline routes)]
    (doseq [polyline polylines]
      (.addTo polyline leaflet-map))))

(def leaflet-map-atom (atom nil))

(defn- map-did-mount
  [map-atom component]
  (print "mounted")
  (let [routes (:routes (reagent/props component))
        initial-lat-lng #js [51.437382 -2.590950]
        leaflet-map (js/L.map "map" (clj->js {:center initial-lat-lng
                                              :zoom 14}))]
    (draw-tile-layer leaflet-map)
    (draw-routes routes leaflet-map)
    (reset! map-atom leaflet-map)
    ; TODO - sort out all these atoms
    (reset! leaflet-map-atom leaflet-map)))

(defn- map-did-update
  [map-atom component]
  (print "updated")
  (let [routes (:routes (reagent/props component))]
    (draw-routes routes @map-atom)))

(defn- leaflet-map []
  (let [map-atom (reagent/atom nil)]
    (reagent/create-class
      {:reagent-render (fn [] [:div#map {:style {:height "80vh"}}
                               ])
       :component-did-mount (partial map-did-mount map-atom)
       :component-did-update (partial map-did-update map-atom)})))

(defn saved-routes-panel []
  (let [routes (re-frame/subscribe [::subs/saved-routes])
        polylines (reduce (fn [polylines {:keys [id] :as route}]
                            (assoc polylines id (route->leaflet-polyline route)))
                          {}
                          @routes)]
    (print "polylines" polylines)
    [:div.columns
     [:div.column
      [leaflet-map {:routes @routes}]]
     [:div.column.is-one-third
      [:h1.title.is-3 "Saved routes:"]
      (for [{:keys [id name distance]} @routes]
        ^{:key id}
        [:div.columns.saved-route-column
         {:on-mouse-over (fn [_]
                           (.fitBounds @leaflet-map-atom (.getBounds (polylines id))))}
         [:div.column (str "Route " id ", distance " distance)]
         [:div.column
          [:button
           {:on-click (fn [_] (re-frame/dispatch [:delete-route id]))}
           "X"]]])]]))