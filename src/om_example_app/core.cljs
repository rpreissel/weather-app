(ns om-example-app.core
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.dom :as gdom]
            [devtools.core :as devtools]
            [om-example-app.corecomponents :as cc]
            [om-example-app.weather :as we]
            [goog.string :as gstring]
            [goog.string.format :as gformat]
            [ajax.core :refer [GET]]))

(enable-console-print!)


; this enables additional features, :custom-formatters is enabled by default
(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)


;;routing comes from https://github.com/jdubie/om-next-router-example

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default
  [{:keys [state]} key _]
  (println "def")
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

(defmethod read :new-city
  [{:keys [state]} _ _]
  (let [{:keys [new-city] :as weather} (get-in @state [:weather :default])]
    {:value new-city}))


(defmethod read :weather
  [{:keys [state ast]} _ {:keys [city] :as params}]
  (let [{:keys [fetch-city data] :as weather} (get-in @state [:weather :default])]
    (if (= fetch-city city)
      {:value weather}
      {:openweather ast})))

(defmethod mutate 'weather/set-new-city
  [{:keys [state]} _ {:keys [city]}]
  {:action #(swap! state assoc-in [:weather :default :new-city] city)})

(defmethod mutate 'weather/load-new-city
  [{:keys [state]} _ _]
  (let [{:keys [new-city] :as current-weather} (get-in @state [:weather :default])
        new-weather (assoc current-weather :city new-city
                                           :new-city nil
                                           :data nil)]
    {:action #(swap! state assoc-in [:weather :default] new-weather)}))


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

(def route->component
  {:index   HomePage
   :weather we/Weather})

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
               (dom/div #js {:className "ApplicationView"}
                        (view-factory route-props))))))



(defonce app-state (atom {:current-route      :index
                          :weather {:default {:data {} :new-city ""}}}))

(def api-key "444112d540b141913a9c1ee6d7f495fa")

(defn fetch-weather [{:keys [openweather]} cb]
  (println "fetch: " openweather)
  (let [{[ast] :children} (om/query->ast openweather)
        city (get-in ast [:params :city])]
    (GET (gstring/format "http://api.openweathermap.org/data/2.5/weather?q=%s,de&appid=%s&units=metric" city api-key)
         {:handler         #(do
                             (println "Success: " %)
                             (cb {[:weather :default] {:data % :fetch-city city}}))
          :error-handler   #(do
                             (println "Error: " %)
                             (cb {[:weather :default] {:error (:status-text %)}}))
          :response-format :json
          :keywords?       true})))

(def reconciler
  (om/reconciler
    {:state   app-state
     :parser  (om/parser {:read read :mutate mutate})
     :remotes [:openweather]
     :send    fetch-weather
     }))




(om/add-root! reconciler
              we/Weather (gdom/getElement "app"))



