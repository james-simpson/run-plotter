(ns run-plotter.views.base
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [run-plotter.routes :as routes]
    [run-plotter.views.edit-route :refer [edit-route-panel]]
    [run-plotter.views.saved-routes :refer [saved-routes-panel]]))

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
  (radio-buttons {:name "units"
                  :selected-value units
                  :options [[:km "km"] [:miles "miles"]]
                  :on-change (fn [value] (re-frame/dispatch [:change-units value]))}))

(defn- navbar-item
  [route text active-panel]
  [:a.navbar-item {:href (routes/url-for route)
                   :class (if (= active-panel route) "is-active" "")}
   text])

(defn- navbar
  [active-panel units]
  [:nav.navbar.is-info {:style {:height "60px"}}
   [:div.navbar-brand
    [:a.navbar-item {:href (routes/url-for :edit-route)
                     :style {:padding-left "20px"}}
     [:img {:src "img/runner-icon.svg"
            :style {:max-height "2em"}}]]]
   [:div.navbar-menu
    [:div.navbar-start
     [navbar-item :edit-route "Create a route" active-panel]
     [navbar-item :saved-routes "Saved routes" active-panel]]
    [:div.navbar-end
     [:div.navbar-item
      [units-toggle units]]
     [:a.navbar-item {:href "https://github.com/jsimpson-github/run-plotter"
                      :style {:margin-right "20px"}}
      [:img {:src "img/github-logo.svg"}]]]]])

(defn base-view []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])
        units (re-frame/subscribe [::subs/units])]
    [:div
     [navbar @active-panel @units]
     [:div {:style {:padding "25px"
                    :background-color "#f9f9f6"}}
      (case @active-panel
        :edit-route [edit-route-panel]
        :saved-routes [saved-routes-panel]
        [edit-route-panel])]]))