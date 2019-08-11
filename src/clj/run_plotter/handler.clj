(ns run-plotter.handler
  (:require [compojure.core :refer [GET POST DELETE defroutes]]
            [compojure.route :refer [resources not-found]]
            [integrant.core :as ig]
            [ring.util.response :refer [resource-response]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [run-plotter.db :as db]))

(defn ->routes
  [db-spec]
  (defroutes
    routes
    (GET "/" [] (resource-response "index.html" {:root "public"}))

    (resources "/")

    (GET "/ping" [] "pong")

    (GET "/routes" []
      {:status 200
       :body (db/get-all-routes db-spec)})

    (POST "/routes" req
      (let [route (:body req)
            route-id (db/insert-route! db-spec route)]
        {:status 201
         :body {:id route-id}}))

    (GET "/routes/:id" [id]
      {:status 200
       :body (->> (Integer/parseInt id)
                  (db/get-route db-spec))})

    (DELETE "/routes/:id" [id]
      {:status 200
       :body (->> (Integer/parseInt id)
                  (db/delete-route! db-spec))})

    ;(not-found "Not found")

    (GET "*" [] (resource-response "index.html" {:root "public"}))
    ))

(defmethod ig/init-key ::handler
  [_ {:keys [db-client]}]
  (-> (->routes (:spec db-client))
      (wrap-json-body {:keywords? true})
      (wrap-cors :access-control-allow-origin [#"http://localhost:8280"]
                 :access-control-allow-methods [:get :put :post :delete])
      wrap-json-response))