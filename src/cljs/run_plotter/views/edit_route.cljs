(ns run-plotter.views.edit-route
  (:require
    [re-frame.core :as rf]
    [run-plotter.subs :as subs]
    [run-plotter.utils :as utils]
    [run-plotter.config :as config]
    [reagent.core :as reagent]
    [goog.object]
    [react-leaflet :as react-leaflet]
    ["react" :as react]
    ["leaflet.icon.glyph"]
    ["leaflet-polylinedecorator"]))

(defn- distance-panel
  [value-in-meters units]
  [:h3.subtitle {:style {:position "absolute"
                         :top "70px"
                         :right "10px"
                         :z-index 401
                         :padding "10px"
                         :color "white"
                         :border-radius "7px"
                         :background "#00000080"}}
   (utils/format-distance value-in-meters units 3 true)])

(defn- radio-buttons
  [{:keys [name selected-value on-change options]}]
  [:div.field
   (mapcat (fn [[value text]]
             [^{:key value}
              [:input.is-checkradio {:type "radio"
                                     :name name
                                     :id value
                                     :checked (= value selected-value)
                                     :on-change #(on-change value)}]
              ^{:key (str value "-label")}
              [:label {:for value} text]])
           options)])

(defn units-toggle
  [units]
  [:div.units-toggle
   [:button.button
    {:on-click #(rf/dispatch [:change-units :km])
     :class (if (= units :km) "selected")}
    "km"]
   [:button.button
    {:on-click #(rf/dispatch [:change-units :miles])
     :class (if (= units :miles) "selected")}
    "miles"]])

(defn- centre-map!
  [state]
  (js/navigator.geolocation.getCurrentPosition
    (fn [position]
      (let [co-ords (.-coords position)
            lat (.-latitude co-ords)
            lng (.-longitude co-ords)
            zoom 16
            map-obj (:map-obj @state)]
        (rf/dispatch [:set-location [lat lng]])
        (.setView map-obj #js [lat lng] zoom)))))

(defn centre-button
  [state]
  [:button.button.centre-map
   {:on-click #(centre-map! state)} "Centre"])

(defn- route-operations-panel
  [undos? redos? offer-return-routes?]
  [:div.button-panel
   [:div
    [:button.button
     {:on-click #(rf/dispatch [:initiate-save])} "Save"]
    [:button.button
     {:on-click #(rf/dispatch [:clear-route])} "Clear"]
    [:button.button
     {:on-click #(rf/dispatch [:undo])
      :disabled (not undos?)} "Undo"]
    [:button.button
     {:on-click #(rf/dispatch [:redo])
      :disabled (not redos?)} "Redo"]]
   [:div {:style {:margin-top "6px"}}
    [:button.button
     {:on-click #(rf/dispatch [:plot-shortest-return-route])
      :disabled (not offer-return-routes?)}
     "Back to start"]
    [:button.button
     {:on-click #(rf/dispatch [:plot-same-route-back])
      :disabled (not offer-return-routes?)}
     "Same route back"]]])

(defn- save-route-modal
  [show-save-form? route-name]
  (let [cancel-fn #(rf/dispatch [:cancel-save])
        confirm-fn #(rf/dispatch [:confirm-save])]
    [:div.modal {:style {:z-index 1000}
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
                      (rf/dispatch [:route-name-updated e.target.value]))}]]
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
    :on-change (fn [e] (rf/dispatch
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

(defn edit-route-panel
  []
  (let [state (atom {})]
    (reagent/create-class
      {:display-name "edit-route-panel"
       :component-did-mount #(centre-map! state)
       :reagent-render
       (fn []
         (let [ref-fn (fn [el]
                        (swap! state assoc :map-obj (if el (.-leafletElement el)))
                        (rf/dispatch [:set-map-obj (if el (.-leafletElement el))]))
               centre (rf/subscribe [::subs/centre])
               zoom (rf/subscribe [::subs/zoom])
               device-location (rf/subscribe [::subs/device-location])
               co-ords (rf/subscribe [::subs/co-ords])
               ; the :undos? and :redos? subscriptions are added by the re-frame-undo
               ; library, along with the :undo and :redo event handlers
               undos? (rf/subscribe [:undos?])
               redos? (rf/subscribe [:redos?])
               offer-return-routes? (rf/subscribe [::subs/offer-return-routes?])
               distance (rf/subscribe [::subs/distance])
               route-name (rf/subscribe [::subs/name])
               units (rf/subscribe [::subs/units])
               save-in-progress? (rf/subscribe [::subs/save-in-progress?])
               route-time (rf/subscribe [::subs/route-time])]
           [:div
            [Map {:ref ref-fn
                  :center @centre
                  :zoom @zoom
                  :style {:height "95vh"}
                  :on-click (fn [^js/mapClickEvent e]
                              (let [[lat lng] [e.latlng.lat e.latlng.lng]]
                                (rf/dispatch [:add-waypoint lat lng])))}

             [TileLayer {:url (str "https://api.tiles.mapbox.com/styles/v1/mapbox/outdoors-v11/tiles/256/{z}/{x}/{y}?access_token=" config/mapbox-token)
                         :attribution "Map data &copy; <a href=\"https://www.openstreetmap.org/\">OpenStreetMap</a> contributors, Imagery © <a href=\"https://www.mapbox.com/\">Mapbox</a>"}]

             [PolylineDecorator {:co-ords @co-ords}]

             (if-let [location @device-location]
               [Marker {:position location
                        :icon (js/L.icon.glyph #js {:glyph "X"})}])

             (if-let [start (first @co-ords)]
               [Marker {:position start
                        :icon (js/L.icon.glyph #js {:glyph "A"})}])

             (if-let [end (last (rest @co-ords))]
               [Marker {:position end
                        :icon (js/L.icon.glyph #js {:glyph "B"})}])]
            [distance-panel @distance @units]
            [units-toggle @units]
            [centre-button state]
            [route-operations-panel @undos? @redos? @offer-return-routes?]
            [:div
             [pace-calculator @distance @route-time]]
            [save-route-modal @save-in-progress? @route-name]]))})))