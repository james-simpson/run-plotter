(ns run-plotter.handler
  (:require [compojure.core :refer [GET POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [integrant.core :as ig]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [run-plotter.db :as db]))


(defn ->routes
  [db-spec]
  (defroutes
    routes
    (GET "/ping" [] "pong")

    (GET "/routes" []
      {:status 200
       :body (db/get-all-routes db-spec)})

    (POST "/routes" req
      (let [{:keys [name distance waypoints]} (:body req)
            route-id (db/insert-route! db-spec name distance waypoints)]
        {:status 201
         :body {:id route-id}}))

    (GET "/routes/:id" [id]
      {:status 200
       :body (->> (Integer/parseInt id)
                  (db/get-route db-spec))})

    (DELETE "/routes/:id" [id]
      {:status 200
       :body (->> (Integer/parseInt id)
                  (db/delete-route! db-spec))})))

(defroutes dev-routes
           (GET "/ping" [] "pong"))

(def dev-handler (-> #'dev-routes wrap-reload))

(defmethod ig/init-key ::handler
  [_ {:keys [db-client]}]
  (-> (->routes (:spec db-client))
      (wrap-json-body {:keywords? true})
      wrap-json-response))