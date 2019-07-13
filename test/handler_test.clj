(ns handler-test
  (:require
    [pg-embedded-clj.core :as pg-embedded]
    [run-plotter.config :as config]
    [run-plotter.db :as db]
    [integrant.core :as ig]
    [integrant.repl :as igr]
    [integrant.repl.state :as ig-state]
    [clj-http.client :as http]
    [clojure.test :refer :all]
    [clojure.data.json :as json]))

(defn- get-db-spec
  []
  (get-in ig-state/system [:duct.database.sql/hikaricp :spec]))

(defn around-all
  [f]
  (pg-embedded/with-pg-fn
    {:port 54321 :log-redirect nil}
    (fn []
      (try
        (let [conf (-> (config/read-config)
                       (assoc-in [:duct.database.sql/hikaricp :port-number] 54321))]
          (igr/set-prep! (constantly conf))
          (ig/load-namespaces conf)
          (igr/go))
        (f)
        (catch Exception e
          (clojure.stacktrace/print-stack-trace e))
        (finally
          (ig/halt! ig-state/system))))))

(use-fixtures :each around-all)

(def ^:private base-url "http://localhost:3000")

(deftest ping
  (let [response (http/get (str base-url "/ping"))]
    (is (= 200 (:status response)))
    (is (= "pong" (:body response)))))

(deftest get-routes
  (let [db-spec (get-db-spec)]
    (db/insert-route! db-spec "Bristol 10k" 10000 [[60.1 70.2] [60.2 70.3]])
    (db/insert-route! db-spec "London marathon" 40000 [[15.0 40.2] [15.0 40.5]]))

  (testing "GET /routes"
    (let [response (http/get (str base-url "/routes")
                             {:as :json})]
      (is (= 200 (:status response)))
      (is (= [{:id 1 :name "Bristol 10k" :distance 10000}
              {:id 2 :name "London marathon" :distance 40000}]
             (:body response)))))

  (testing "GET /routes/:id"
    (let [response (http/get (str base-url "/routes/1")
                             {:as :json})]
      (is (= 200 (:status response)))
      (is (= {:id 1
              :name "Bristol 10k"
              :distance 10000
              :waypoints [[60.1 70.2] [60.2 70.3]]}
             (:body response))))))

(deftest post-routes
  (testing "POST /routes"
    (let [db-spec (get-db-spec)
          route {:name "Bristol 10k"
                 :distance 10000
                 :waypoints [[60.1 70.2] [60.2 70.3]]}
          response (http/post (str base-url "/routes")
                              {:body (json/write-str route)
                               :content-type :json
                               :as :json})]
      (is (= 201 (:status response)))
      (is (= {:id 1
              :name "Bristol 10k"
              :distance 10000
              :waypoints [[60.1M 70.2M] [60.2M 70.3M]]}
             (db/get-route db-spec 1))))))

(deftest delete-route
  (testing "DELETE /delete/:id"
    (let [db-spec (get-db-spec)
          _ (db/insert-route! db-spec "Bristol 10k" 10000 [[60.1 70.2] [60.2 70.3]])
          _ (db/insert-route! db-spec "London marathon" 40000 [[15.0 40.2] [15.0 40.5]])
          response (http/delete (str base-url "/routes/2")
                                {:as :json})]
      (is (= 200 (:status response)))
      (is (= 1 (count (db/get-all-routes db-spec)))))))
