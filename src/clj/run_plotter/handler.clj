(ns run-plotter.handler
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [resources]]
            [integrant.core :as ig]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [run-plotter.db :as db]
            [clojure.data.json :as json]))


(defn ->routes
  [db-spec]
  (defroutes
    routes
    (GET "/ping" [] "pong")

    (GET "/routes" []
      {:status 200
       :body (json/write-str (db/get-all-routes db-spec))})

    (GET "/routes/:id" [id]
      {:status 200
       :body (->> (Integer/parseInt id)
                  (db/get-route db-spec)
                  json/write-str)})))

(defroutes dev-routes
           (GET "/ping" [] "pong"))

(def dev-handler (-> #'dev-routes wrap-reload))

(defmethod ig/init-key ::handler
  [_ {:keys [db-client]}]
  (->routes (:spec db-client)))