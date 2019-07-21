(defproject run-plotter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [day8.re-frame/undo "0.3.2"]
                 [kibu/pushy "0.3.8"]
                 [bidi "2.1.6"]
                 [cljs-http "0.1.46"]
                 [io.jesi/clojure-polyline "0.4.1"]
                 ;[cljsjs/leaflet "1.5.1-0"]

                 [compojure "1.6.1"]
                 [yogthos/config "1.1.2"]
                 [ring "1.7.1"]
                 [ring/ring-json "0.4.0"]
                 [ring-cors "0.1.13"]
                 [ragtime "0.8.0"]
                 [integrant "0.7.0"]
                 [aero "1.1.3"]
                 [duct/module.sql "0.5.0"]
                 [org.postgresql/postgresql "42.2.5"]
                 [com.layerware/hugsql "0.4.9"]
                 [honeysql "0.9.4"]]

  :plugins [[lein-cljsbuild "1.1.7"]]

  :main run-plotter.core

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler run-plotter.user/figwheel-handler}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]
                   [day8.re-frame/re-frame-10x "0.4.0"]
                   [day8.re-frame/tracing "0.5.1"]
                   [ring/ring-mock "0.4.0"]
                   [bigsy/pg-embedded-clj "0.0.8"]
                   [integrant/repl "0.3.1"]
                   [clj-http "3.9.1"]]

    :plugins      [[lein-figwheel "0.5.18"]]}
   :prod { :dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]}
   :uberjar {:source-paths ["env/prod/clj"]
             :dependencies [[day8.re-frame/tracing-stubs "0.5.1"]]
             :omit-source  true
             :main         run-plotter.server
             :aot          [run-plotter.server]
             :uberjar-name "run-plotter.jar"
             :prep-tasks   ["compile" ["cljsbuild" "once" "min"]]}
   }

  :cljsbuild
  {:builds
   [{:id "dev"
     :source-paths ["src/cljs"]
     :figwheel {:on-jsload "run-plotter.core/mount-root"}
     :compiler {:main run-plotter.core
                :output-to "resources/public/js/compiled/app.js"
                :output-dir "resources/public/js/compiled/out"
                :asset-path "js/compiled/out"
                :source-map-timestamp true
                :preloads [devtools.preload
                           day8.re-frame-10x.preload]
                :closure-defines {"re_frame.trace.trace_enabled_QMARK_" true
                                  "day8.re_frame.tracing.trace_enabled_QMARK_" true}
                :external-config {:devtools/config {:features-to-install :all}}
                }}

    {:id "min"
     :source-paths ["src/cljs"]
     :jar true
     :compiler {:main run-plotter.core
                :output-to "resources/public/js/compiled/app.js"
                :optimizations :advanced
                :externs ["resources/leaflet.js"
                          "resources/leaflet-routing-machine.js"]
                :closure-defines {goog.DEBUG false}}}

    ]}
  )
