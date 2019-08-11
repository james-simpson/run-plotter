(ns run-plotter.views.saved-routes
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [run-plotter.utils :as utils]
    [reagent.core :as reagent]
    [com.michaelgaare.clojure-polyline :as polyline]
    [react-leaflet :as react-leaflet]))

(def Map (reagent/adapt-react-class react-leaflet/Map))
(def TileLayer (reagent/adapt-react-class react-leaflet/TileLayer))
(def Polyline (reagent/adapt-react-class react-leaflet/Polyline))

(def polyline-styles
  {:color "grey"
   :dashArray "4"})

(def highlighted-polyline-styles
  {:color "red"
   :dashArray "none"})

(defn- highlight-polyline
  [state id]
  (let [{leaflet-map :leaflet polylines-by-id :polylines} state
        polyline (polylines-by-id id)
        other-polylines (vals (dissoc polylines-by-id id))]
    (.fitBounds leaflet-map (.getBounds polyline))
    (.setStyle polyline (clj->js highlighted-polyline-styles))
    (.bringToFront polyline)
    (doseq [p other-polylines]
      (.setStyle p (clj->js polyline-styles)))))

(defn saved-routes-panel []
  ; Using refs here to access the leaflet map & polyline objects directly
  ; to do things like call fitBounds and setStyle etc. when you hover over a route.
  ; This avoids re-rendering the component, which was a bit slow.
  (let [state (atom {})
        ref-fn (fn [el] (swap! state assoc :leaflet (if el (.-leafletElement el))))
        polyline-ref-fn (fn [id el] (swap! state assoc-in [:polylines id] (if el (.-leafletElement el))))]
    (fn render-fn []
      (let [routes @(re-frame/subscribe [::subs/saved-routes])
            units @(re-frame/subscribe [::subs/units])
            polylines-by-id (reduce (fn [polys {id :id polystring :polyline}]
                                      (assoc polys id (polyline/decode polystring)))
                                    {}
                                    routes)]
        [:div.columns
         [:div.column
          [Map {:ref ref-fn
                :center [51.437382 -2.590950]
                :zoom 14
                :style {:height "80vh"}}

           [TileLayer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                       :attribution "© OpenStreetMap contributors"}]

           (for [[id poly-co-ords] polylines-by-id]
             [Polyline (merge {:ref (partial polyline-ref-fn id)
                               :key id
                               :positions poly-co-ords}
                              polyline-styles)])]]

         [:div.column.is-one-third
          [:div.panel
           [:p.panel-heading "Saved routes"]
           [:div.panel-block
            [:table.table.saved-routes-table
             [:thead
              [:tr
               [:td {:style {:width "50%"}} "Route"]
               [:td (str "Distance (" (name units) ")")]
               [:td]]]
             [:tbody
              (for [{:keys [id name distance]} routes]
                ^{:key id}
                [:tr.saved-route-row
                 {:on-mouse-over (fn [_] (highlight-polyline @state id))}
                 [:td name]
                 [:td (utils/format-distance distance units)]
                 [:td [:button.delete
                       {:on-click (fn [_] (re-frame/dispatch [:delete-route id]))}]]])]]]]]]))))