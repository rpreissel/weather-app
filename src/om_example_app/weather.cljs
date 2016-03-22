(ns om-example-app.weather
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.string :as gstring]
            [goog.string.format :as gformat]
            [goog.dom :as gdom]
            [ajax.core :refer [GET]]))

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
    '[:new-city (:weather {:city ?city})])
  Object
  (render [this]
    (println "render weather")
    (let [{new-city :new-city {:keys [data error]} :weather} (om/props this)]
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

(defonce app-state (atom {:new-city ""
                          :weather {}}))

(defmethod read :new-city
  [{:keys [state]} _ _]
  (let [new-city (:new-city @state)]
    {:value new-city}))


(defmethod mutate 'weather/set-new-city
  [{:keys [state]} _ {:keys [city]}]
  {:action #(swap! state assoc :new-city city)})

(defmethod read :weather
  [{:keys [state ast]} _ {:keys [city]}]
  (let [{:keys [fetched-city] :as weather} (get-in @state [:weather :default])]
    (if (= fetched-city city)
      {:value weather}
      {:fetchweather ast})))


(def api-key "444112d540b141913a9c1ee6d7f495fa")
(defn fetch-weather [{:keys [fetchweather]} cb]
  (println "fetch: " fetchweather)
  (let [{[ast] :children} (om/query->ast fetchweather)
        city (get-in ast [:params :city])]
    (GET (gstring/format "http://api.openweathermap.org/data/2.5/weather?q=%s,de&appid=%s&units=metric" city api-key)
         {:handler         #(do
                             (println "Success: " %)
                             (cb {[:weather :default] {:data % :fetched-city city}}))
          :error-handler   #(do
                             (println "Error: " %)
                             (cb {[:weather :default] {:error (:status-text %)}}))
          :response-format :json
          :keywords?       true})))

(def reconciler
  (om/reconciler
    {:state   app-state
     :parser  (om/parser {:read read :mutate mutate})
     :remotes [:fetchweather]
     :send    fetch-weather
     }))

(defn add-root! [elem]
  (om/add-root! reconciler
                Weather (gdom/getElement elem)))

