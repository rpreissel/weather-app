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


;;routing comes from https://github.com/jdubie/om-next-router-example

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default
  [{:keys [state]} key _]
  (let [st @state]
    (if-let [[_ v] (find st key)]
      {:value v}
      {:value :not-found})))


(defmethod read :current-route
  [{:keys [state]} _ _]
  {:value (get @state :current-route)})

(defmethod read :route-props
  [{:keys [state query parser] :as env} _ _]
  (let [{:keys [current-route]} @state]
    "Here `query` is a map from route -> query. So we only use the query for the current route"
    {:value (parser (dissoc env :query)
                    (get query current-route))}))

(defmethod mutate 'navigation/set-current-route
  [{:keys [state]} _ {:keys [route]}]
  {:value  {:keys [:current-route]}
   :action #(swap! state assoc :current-route route)})

(defui HomePage
  static om/IQuery
  (query [this]
    [:page/home])
  Object
  (render [this]
    (dom/div nil "HomePage")))

(defui Weather
  static om/IQuery
  (query [this]
    [:page/weather])
  Object
  (render [this]
    (dom/div nil "Weather")))

(def route->component
  {:index   HomePage
   :weather Weather})

(def route->title
  [:index "View1"
   :weather "View2"])

(def route->factory
  (zipmap (keys route->component)
          (map om/factory (vals route->component))))

(def route->query
  (zipmap (keys route->component)
          (map om/get-query (vals route->component))))



(defui Application
  static om/IQuery
  (query [_]
    "This is called the \"Total Query\" approach because `route->query` is the complete
    query of the app."
    [:current-route {:route-props route->query}])
  Object
  (render [this]
    (let [{:keys [current-route route-props]} (om/props this)
          view-factory (route->factory current-route)]
      (dom/div nil
               (cc/navigation-bar {:views         route->title
                                   :current-route current-route
                                   :on-click      #(om/transact! this `[(navigation/set-current-route {:route ~%})])})
               (view-factory route-props)))))



(defonce app-state (atom {:current-route :index}))

(def reconciler
  (om/reconciler
    {:state  app-state
     :parser (om/parser {:read read :mutate mutate})
     }))




(om/add-root! reconciler
              Application (gdom/getElement "app"))



