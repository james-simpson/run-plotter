(ns run-plotter.routes
  (:require
    [bidi.bidi :as bidi]
    [pushy.core :as pushy]
    [re-frame.core :as re-frame]
    [run-plotter.events :as events]))

(def routes ["/" {"" :edit-route
                  ["edit/" :id] :edit-route
                  "saved" :saved-routes}])

(def url-for (partial bidi/path-for routes))

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn- dispatch-route [matched-route]
  (let [panel-name (:handler matched-route)
        params (:route-params matched-route)]
    (if-let [route-id (:id params)]
      (re-frame/dispatch [:load-route route-id])
      (re-frame/dispatch [:set-active-panel panel-name]))))

(def history (pushy/pushy dispatch-route parse-url))

(defn set-url [url]
  (pushy/set-token! history url))

(defn listen-for-url-changes! []
  (pushy/start! history))
