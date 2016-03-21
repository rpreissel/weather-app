(ns om-example-app.core
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [devtools.core :as devtools]
            [reagent.core :as reagent]
            [om-example-app.corecomponents :as cc]
            [om-example-app.password :as pw]
            [om-example-app.weather :as we]
            [om-example-app.chart :as ch]))

(enable-console-print!)

; this enables additional features, :custom-formatters is enabled by default
(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)


(defonce app-state (atom {:navigation {:views ["View1" #(dom/div nil "Hello1")
                                               "View2" #(dom/div nil "Hello2")]
                                       :current-view 0}}))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default
  [{:keys [state]} key _]
  (let [st @state]
    (if-let [[_ v] (find st key)]
      {:value v}
      {:value :not-found})))

(defmethod mutate 'navigation/set-current-view
  [{:keys [state] :as env} _ {:keys [view]}]
    {:value {:keys [:navigation :current-view]}
     :action #(swap! state assoc-in [:navigation :current-view] view)})

(def reconciler
  (om/reconciler
    {:state  app-state
     :parser (om/parser {:read read :mutate mutate})
     }))


(comment (def views
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
         )

(defui Application
  static om/IQuery
  (query [this] [{:navigation (om/get-query cc/NavigationBar)}])
  Object
  (render [this]
    (dom/div nil (cc/navigation-bar (:navigation (om/props this))))))

(om/add-root! reconciler
              Application (gdom/getElement "app"))



