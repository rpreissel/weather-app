(ns weather-app.om
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [goog.string :as gstring]
            [goog.string.format :as gformat]
            [goog.dom :as gdom]
            [ajax.core :refer [GET]]))

(defui CityInputPanel
  static om/IQuery
  (query [_]
    '[:city])
  Object
  (render [this]
    (let [{:keys [city]} (om/props this)
          {:keys [on-load]} (om/get-computed this)]
      (println "render city input panel")
      (dom/div nil
               (dom/h1 nil "Current-Weather")
               (dom/input #js {:type     "text"
                               :focus    true
                               :value    city
                               :onChange #(om/transact! this `[(weather/set-input-city {:city ~(-> % .-target .-value)})])})
               (dom/button #js {:disabled (-> city count pos? not)
                                :onClick  #(on-load city)}
                           "Load")))))

(def city-input-panel (om/factory CityInputPanel))

  (defui WeatherPanel
    Object
    (render [this]
      (println "render weather panel")
      (let [{:keys [weather]} (om/props this)]
        (if-not weather
          (dom/h1 nil "No weather data available")
          (let [current-weather (get-in weather [:weather 0])]
            (dom/div nil
                     (dom/h1 nil (str "Weather in " (:name weather)))
                     (dom/h2 nil (str (get-in weather [:main :temp]) " °C")
                             (dom/img #js {:src (str "http://openweathermap.org/img/w/" (:icon current-weather) ".png")}))
                     (dom/p nil (:description current-weather))))))))

(def weather-panel (om/factory WeatherPanel))

(defui WeatherView
  static om/Ident
  (ident [_ _]
    [:weather :default])
  static om/IQueryParams
  (params [_]
    {:city "Hamburg"})
  static om/IQuery
  (query [_]
    `[{:input ~(om/get-query CityInputPanel)} (:weather {:city ~'?city})])
  Object
  (render [this]
    (println "render weather view")
    (let [{input :input {:keys [data error]} :weather} (om/props this)
          cinput (om/computed input {:on-load #(om/set-query! this {:params {:city %}})})]
      (println "Data: " data)
      (println "Error: " error)
      (dom/div #js {:className "ApplicationView"}
               (city-input-panel cinput)
               (when data (weather-panel {:weather data}))
               (when error (dom/div #js {:className "Red"} (str "Error: " error)))))))

(defonce app-state (atom {:input   {:city "Hamburg"}
                          :weather {}}))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :input
  [{:keys [state]} _ _]
  (let [input (:input @state)]
    {:value input}))


(defmethod mutate 'weather/set-input-city
  [{:keys [state]} _ {:keys [city]}]
  {:action #(swap! state assoc-in [:input :city] city)})

(defmethod read :weather
  [{:keys [state ast]} _ {:keys [city]}]
  (let [{:keys [fetched-city] :as weather} (get-in @state [:weather :default])]
    (if (= fetched-city city)
      {:value weather}
      {:remote ast})))

(def api-key "444112d540b141913a9c1ee6d7f495fa")
(defn fetch-weather [{:keys [remote]} cb]
  (println "fetch: " remote)
  (let [{[ast] :children} (om/query->ast remote)
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
    {:state  app-state
     :parser (om/parser {:read read :mutate mutate})
     :send   fetch-weather}))

(defn add-root! [elem]
  (om/add-root! reconciler
                WeatherView (gdom/getElement elem)))