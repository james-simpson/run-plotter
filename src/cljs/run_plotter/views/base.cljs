(ns run-plotter.views.base
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [run-plotter.routes :as routes]
    [run-plotter.views.edit-route :refer [edit-route-panel]]
    [run-plotter.views.saved-routes :refer [saved-routes-panel]]
    [reagent.core :as reagent]))

(defn- navbar-item
  [active-panel menu-expanded-atom route text]
  [:a.navbar-item {:href (routes/url-for route)
                   :class (if (= active-panel route) "is-active" "")
                   :on-click #(reset! menu-expanded-atom false)}
   text])

(defn- navbar
  [active-panel units]
  (let [menu-expanded? (reagent/atom false)]
    (fn [active-panel units]
      (let [menu-item (partial navbar-item active-panel menu-expanded?)]
        [:nav.navbar.is-info
         [:div.navbar-brand
          [:a.navbar-item {:href (routes/url-for :edit-route)
                           :style {:padding-left "20px"}}
           [:img {:src "img/runner-icon.svg"}]]
          [:div.navbar-burger.burger
           {:class (if @menu-expanded? "is-active" "")
            :on-click #(swap! menu-expanded? not)}
           [:span] [:span] [:span]]]
         [:div.navbar-menu {:class (if @menu-expanded? "is-active" "")}
          [:div.navbar-start
           [menu-item :edit-route "Create a route"]
           [menu-item :saved-routes "Saved routes"]]
          [:div.navbar-end
           [:a.navbar-item {:href "https://github.com/jsimpson-github/run-plotter"
                            :style {:margin-right "20px"}}
            (if @menu-expanded?
              "Github project"
              [:img {:src "img/github-logo.svg"}])]]]]))))

(defn base-view []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])
        units (re-frame/subscribe [::subs/units])]
    [:div
     [navbar @active-panel @units]
     [:div
      (case @active-panel
        :edit-route [edit-route-panel]
        :saved-routes [saved-routes-panel]
        [edit-route-panel])]]))