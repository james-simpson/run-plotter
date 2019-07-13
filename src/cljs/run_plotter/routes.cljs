(ns run-plotter.routes
  (:require
    [bidi.bidi :as bidi]
    [pushy.core :as pushy]
    [re-frame.core :as re-frame]
    [run-plotter.events :as events]))

(def routes ["/" {"" :edit-route
                  "saved" :saved-routes}])

(defn- parse-url [url]
  (bidi/match-route routes url))

(defn- dispatch-route [matched-route]
  (print "change route to" matched-route)
  (let [panel-name (:handler matched-route)]
    (re-frame/dispatch [:set-active-panel panel-name])))

(defn listen-for-url-changes! []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))
