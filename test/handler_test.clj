(ns handler-test
  (:require
    [pg-embedded-clj.core :as pg-embedded]
    [run-plotter.config :as config]
    [run-plotter.db :as db]
    [integrant.core :as ig]
    [clj-http.client :as http]
    [clojure.test :refer :all]))

(def system-atom (atom nil))

(defn- get-db-conn
  []
  (get-in @system-atom [:duct.database.sql/hikaricp :spec]))

(defn around-all
  [f]
  (pg-embedded/with-pg-fn
    {:port 54321 :log-redirect nil}
    (fn []
      (try
        (let [system-config (config/read-config)
              _ (ig/load-namespaces system-config)
              system (ig/init system-config)]
          (reset! system-atom system)
          (f))
        (catch Exception e
          (clojure.stacktrace/print-stack-trace e))
        (finally
          (ig/halt! @system-atom))))))

(use-fixtures :once around-all)

(def ^:private base-url "http://localhost:3000")

(deftest ping
  (let [response (http/get (str base-url "/ping"))]
    (is (= 200 (:status response)))
    (is (= "pong" (:body response)))))

(deftest get-routes
  (let [db-conn (get-db-conn)]
    (db/insert-route! db-conn "Bristol 10k" 10000 [[60.1 70.2] [60.2 70.3]])
    (db/insert-route! db-conn "London marathon" 40000 [[15.0 40.2] [15.0 40.5]]))

  (testing "/routes"
    (let [response (http/get (str base-url "/routes")
                             {:as :json})]
      (is (= 200 (:status response)))
      (is (= [{:id 1 :name "Bristol 10k" :distance 10000}
              {:id 2 :name "London marathon" :distance 40000}]
             (:body response)))))

  (testing "/routes/:id"
    (let [response (http/get (str base-url "/routes/1")
                             {:as :json})]
      (is (= 200 (:status response)))
      (is (= {:id 1
              :name "Bristol 10k"
              :distance 10000
              :waypoints [[60.1 70.2] [60.2 70.3]]}
             (:body response))))))
