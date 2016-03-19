(ns om-example-app.core
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [reagent.core :as reagent]
            [om-example-app.corecomponents :as cc]
            [om-example-app.password :as pw]
            [om-example-app.weather :as we]
            [om-example-app.chart :as ch]))

(enable-console-print!)

(defonce app-state (atom {}))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(def reconciler
  (om/reconciler
    {:state app-state
     :parser (om/parser {:read read :mutate mutate})
     }))


(def views
  ["Password Form" [pw/password-view]
   "Weather Report" [we/weather-view]
   "Chart Example" ^{:class "ApplicationView-chartView"} [ch/chart-view]])


(defn application []
  (let [current-view (reagent/atom (views 1))]
    (fn []
      (let [view @current-view
            application-view-class-names (str "ApplicationView " (-> view meta :class))]
        [:div
         [cc/navigation-bar views view #(reset! current-view %)]
         [:div {:class application-view-class-names} view]]))))


(reagent/render-component [application]
                          (. js/document (getElementById "app")))

(defui Application
  Object
  (render [this]
    (dom/div nil (str "Hello World"))))

(om/add-root! reconciler
              Application (gdom/getElement "app"))



