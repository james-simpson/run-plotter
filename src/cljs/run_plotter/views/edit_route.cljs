(ns run-plotter.views.edit-route
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [run-plotter.utils :as utils]
    [reagent.core :as reagent]
    [goog.object]
    [com.michaelgaare.clojure-polyline :as polyline]
    [react-leaflet :as react-leaflet]))

; MAPBOX_TOKEN variable is loaded from resources/js/config.js
(def ^:private mapbox-token js/MAPBOX_TOKEN)

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

(defn edit-route-panel []
  (let [co-ords (re-frame/subscribe [::subs/co-ords])
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
       [Map {:center [51.437382 -2.590950]
             :zoom 17
             :style {:height "535px"}
             :on-click (fn [^js/mapClickEvent e]
                         (re-frame/dispatch [:add-waypoint e.latlng.lat e.latlng.lng]))}

        [TileLayer {:url "http://{s}.tile.osm.org/{z}/{x}/{y}.png"
                    :attribution "© OpenStreetMap contributors"}]

        [Polyline {:positions @co-ords}]

        (if-let [start (first @co-ords)]
          [Marker {:position start
                   ;todo - :icon
                   }])

        (if-let [end (last (rest @co-ords))]
          [Marker {:position end
                   ;todo - :icon
                   }])]
       [distance-panel @distance @units]]
      [:div.column.is-one-third
       [pace-calculator @distance @route-time]]]
     [route-operations-panel @undos? @redos? @offer-return-routes?]
     [save-route-modal @save-in-progress? @route-name]]))