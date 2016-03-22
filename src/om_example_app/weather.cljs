(ns om-example-app.weather
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [devtools.core :as devtools]
            [goog.string :as gstring]
            [goog.string.format :as gformat]
            [goog.dom :as gdom]
            [ajax.core :refer [GET]]))

(enable-console-print!)

; this enables additional features, :custom-formatters is enabled by default
(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)


(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defn weather-panel [weather]
  (if-not weather
    (dom/h1 nil "No weather data available")
    (let [current-weather (get-in weather [:weather 0])]
      (dom/div nil
               (dom/h1 nil (str "Weather in " (:name weather)))
               (dom/h2 nil (str (get-in weather [:main :temp] " °C"))
                       (dom/img #js {:src (str "http://openweathermap.org/img/w/" (:icon current-weather) ".png")}))
               (dom/p nil (:description current-weather))))))


(defui Weather
  static om/Ident
  (ident [this _]
    [:weather :default])
  static om/IQueryParams
  (params [_]
    {:city "Hamburg"})
  static om/IQuery
  (query [_]
    ['(:weather {:city ?city})])
  Object
  (render [this]
    (let [{{:keys [new-city data error]} :weather} (om/props this)
          city (-> this om/get-params :city)]
      (println "Data: " data)
      (println "Error: " error)
      (dom/div #js {:className "ApplicationView"}
               (dom/h1 nil "Current-Weather")
               (dom/input #js {:type     "text"
                               :focus    true
                               :value    new-city
                               :onChange #(om/transact! this `[(weather/set-new-city {:city ~(-> % .-target .-value)})])})
               (dom/button #js {:disabled (-> new-city count pos? not)
                                :onClick  #(do (om/set-query! this {:params {:city new-city}})
                                               (om/transact! this `[(weather/set-new-city "")]))}
                           "Load")
               (when data (weather-panel data))))))

(defonce app-state (atom {:weather
                          {:default
                           {:data     {}
                            :new-city ""}}}))

(defmethod read :weather
  [{:keys [state ast]} _ {:keys [city]}]
  (let [{:keys [fetch-city] :as weather} (get-in @state [:weather :default])]
    (if (= fetch-city city)
      {:value weather}
      {:openweather ast})))

(defmethod mutate 'weather/set-new-city
  [{:keys [state]} _ {:keys [city]}]
  {:action #(swap! state assoc-in [:weather :default :new-city] city)})


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
              Weather (gdom/getElement "app"))
