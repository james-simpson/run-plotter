(ns run-plotter.views.edit-route
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [run-plotter.utils :as utils]
    [reagent.core :as reagent]
    [goog.object]
    [react-leaflet :as react-leaflet]
    ["react" :as react]
    ["leaflet.icon.glyph"]
    ["leaflet-polylinedecorator"]))

; TODO - move to server side and regenerate token
(def mapbox-token "pk.eyJ1IjoianNpbXBzb245MiIsImEiOiJjandzY2ExZDIwbTB3NDRwNWFlZzYyenRvIn0.Vp-UX6Hs7efpjiERiVMVZQ")

(defn- distance-panel
  [value-in-meters units]
  [:h3.subtitle.is-3 {:style {:padding-top "1.5rem"}}
   (utils/format-distance value-in-meters units 3 true)])

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

(defn- zero-pad-duration
  [n]
  (if (< n 10)
    (str "0" n)
    (str n)))

(def ^:private common-distances
  [[1 "km"]
   [1.60934 "mile"]
   [5 "5k"]
   [10 "10k"]
   [(* 1.60934 13.1) "Half marathon"]
   [(* 1.60934 26.2) "Marathon"]])

(defn- format-duration
  [time-in-seconds]
  (let [hours (Math/floor (/ time-in-seconds 3600))
        minutes (Math/floor (/ (- time-in-seconds (* 3600 hours)) 60))
        seconds (mod (Math/round time-in-seconds) 60)
        [h m s] (map zero-pad-duration [hours minutes seconds])]
    (str (if (> hours 0) (str h ":") "") m ":" s)))

(defn- time-input
  [unit value]
  [:input.input
   {:value value
    :on-change (fn [e] (re-frame/dispatch
                         [:route-time-updated unit (int e.target.value)]))}])

(defn- pace-calculator
  [route-distance {:keys [hours mins secs total-seconds]}]
  (let [seconds-per-km (/ total-seconds (/ route-distance 1000))
        common-distance-times (map (fn [[distance label]]
                                     {:label label
                                      :time (format-duration (* distance seconds-per-km))})
                                   common-distances)
        show-results? (and (> total-seconds 0) (> route-distance 0))]
    [:div.panel
     [:p.panel-heading "Pace calculator"]
     [:div.panel-block
      [:div.field
       [:label.label "Time taken to complete route"]
       [:div.pace-inputs
        [:div [:label "hours"] [time-input :hours hours]]
        [:div [:label "mins"] [time-input :mins mins]]
        [:div [:label "secs"] [time-input :secs secs]]]]]
     (if show-results?
       [:div.panel-block
        [:table.table
         [:thead [:tr [:td "Distance"] [:td "Time"]]]
         [:tbody
          (for [{:keys [label time]} common-distance-times]
            ^{:key label}
            [:tr
             [:td label]
             [:td time]])]]])]))

(def Map (reagent/adapt-react-class react-leaflet/Map))
(def TileLayer (reagent/adapt-react-class react-leaflet/TileLayer))
(def Polyline (reagent/adapt-react-class react-leaflet/Polyline))
(def Marker (reagent/adapt-react-class react-leaflet/Marker))

(def polyline-decorator-opts
  (clj->js {:patterns [{:offset 0
                        :repeat 200
                        :symbol (js/L.Symbol.arrowHead
                                  (clj->js {:pixel-size 10
                                            :polygon false
                                            :pathOptions {:color "black"
                                                          :fill-opacity 0.9}}))}]}))

(defn poly-decorator
  [co-ords]
  (let [polyref (react/createRef)
        decorator-atom (atom nil)
        re-render-decorator (fn [_]
                              (let [polyline polyref.current.leafletElement
                                    leaflet-map polyref.current.props.leaflet.map
                                    decorator (js/L.polylineDecorator polyline polyline-decorator-opts)
                                    old-decorator @decorator-atom
                                    _ (reset! decorator-atom decorator)]
                                (.addTo decorator leaflet-map)
                                (when old-decorator
                                  (.removeLayer leaflet-map old-decorator))))]
    (reagent/create-class
      {:component-did-mount re-render-decorator
       :component-did-update re-render-decorator
       :reagent-render
       (fn []
         (let [co-ords (-> (reagent/current-component) reagent/props :coOrds)]
           [Polyline {:ref polyref
                      :color "red"
                      :positions co-ords}]))})))

(def PolylineDecorator
  (reagent/adapt-react-class
    (react-leaflet/withLeaflet (reagent/reactify-component poly-decorator))))

(defn edit-route-panel []
  (let [state (atom {})
        ref-fn (fn [el] (swap! state assoc :map-obj (if el (.-leafletElement el))))
        co-ords (re-frame/subscribe [::subs/co-ords])
        ; the :undos? and :redos? subscriptions are added by the re-frame-undo
        ; library, along with the :undo and :redo event handlers
        undos? (re-frame/subscribe [:undos?])
        redos? (re-frame/subscribe [:redos?])
        offer-return-routes? (re-frame/subscribe [::subs/offer-return-routes?])
        distance (re-frame/subscribe [::subs/distance])
        route-name (re-frame/subscribe [::subs/name])
        units (re-frame/subscribe [::subs/units])
        save-in-progress? (re-frame/subscribe [::subs/save-in-progress?])
        route-time (re-frame/subscribe [::subs/route-time])]
    [:div
     [:div.columns
      [:div.column
       [Map {:ref ref-fn
             :center [51.437382 -2.590950]
             :zoom 16
             :style {:height "70vh"}
             :on-click (fn [^js/mapClickEvent e]
                         (let [[lat lng] [e.latlng.lat e.latlng.lng]]
                           (.panTo (:map-obj @state) #js [lat lng])
                           (re-frame/dispatch [:add-waypoint lat lng])))}

        [TileLayer {:url (str "https://api.tiles.mapbox.com/styles/v1/mapbox/outdoors-v11/tiles/256/{z}/{x}/{y}?access_token=" mapbox-token)
                    :attribution "Map data &copy; <a href=\"https://www.openstreetmap.org/\">OpenStreetMap</a> contributors, Imagery © <a href=\"https://www.mapbox.com/\">Mapbox</a>"
                    :id "mapbox.run-bike-hike"}]

        [PolylineDecorator {:co-ords @co-ords}]

        (if-let [start (first @co-ords)]
          [Marker {:position start
                   :icon (js/L.icon.glyph #js {:glyph "A"})}])

        (if-let [end (last (rest @co-ords))]
          [Marker {:position end
                   :icon (js/L.icon.glyph #js {:glyph "B"})}])]
       [distance-panel @distance @units]]]
     [route-operations-panel @undos? @redos? @offer-return-routes?]
     [:div.column.is-one-third
      [pace-calculator @distance @route-time]]
     [save-route-modal @save-in-progress? @route-name]]))