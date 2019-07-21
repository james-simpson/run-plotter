(ns run-plotter.views.base
  (:require
    [re-frame.core :as re-frame]
    [run-plotter.subs :as subs]
    [run-plotter.routes :as routes]
    [run-plotter.views.edit-route :refer [edit-route-panel]]
    [run-plotter.views.saved-routes :refer [saved-routes-panel]]))

(defn- navbar-item
  [route text active-panel]
  [:a.navbar-item {:href (routes/url-for route)
                   :class (if (= active-panel route) "is-active" "")}
   text])

(defn- navbar
  [active-panel]
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
     [:a.navbar-item {:href "https://github.com/jsimpson-github/run-plotter"
                      :style {:margin-right "20px"}}
      [:img {:src "img/github-logo.svg"}]]]]])

(defn base-view []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [:div
     [navbar @active-panel]
     [:div {:style {:padding "25px"}}
      (case @active-panel
        :edit-route [edit-route-panel]
        :saved-routes [saved-routes-panel]
        [edit-route-panel])]]))