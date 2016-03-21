(ns om-example-app.weather
  (:require [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [reagent.core :as reagent]
            [om-example-app.corecomponents :as cc]
            [goog.string :as gstring]
            [goog.string.format :as gformat]
            [ajax.core :refer [GET]]))


(def api-key "444112d540b141913a9c1ee6d7f495fa")


;(defn weather-panel [weather]
;  (if-not weather
;    [:h1 "No weather data available"]
;    (let [current-weather (get-in weather [:weather 0])]
;      [:div
;       [:h1 (str "Weather in " (:name weather))]
;       [:h2 (str (get-in weather [:main :temp] " °C"))
;        [:img {:src (str "http://openweathermap.org/img/w/" (:icon current-weather) ".png")}]]
;       [:p (:description current-weather)]])))


(defn handle-server-response! [state response]
  (swap! state assoc :weather response :error nil))

(defn handle-server-error! [state {:keys [status status-text]}]
  (swap! state assoc :weather nil :error status-text))

(defn fetch-weather! [state]
  (let [city (:city @state)]
    (GET (gstring/format "http://api.openweathermap.org/data/2.5/weather?q=%s,de&appid=%s&units=metric" city api-key)
         {:handler         (partial handle-server-response! state)
          :error-handler   (partial handle-server-error! state)
          :response-format :json
          :keywords?       true})))

#_(defn weather-view [& {:keys [initial-city] :or {initial-city "Hamburg"}}]
    (let [state (reagent/atom {:city initial-city})]
      (fetch-weather! state)
      (fn []
        (let [{:keys [city weather error]} @state]
          [:div
           [:h1 "Current Weather"]
           [:input {:type      "text" :focus true :value city
                    :on-change #(swap! state assoc :city (-> % .-target .-value))}]
           [cc/button "Load" :enabled (pos? (count city)) :on-click #(fetch-weather! state)]
           (when weather [weather-panel weather])
           (when error [:div.Red (str "Error: " error)])]))))

(defn weather-panel [weather]
  (if-not weather
    (dom/h1 nil "No weather data available")
    (let [current-weather (get-in weather [:weather 0])]
      (dom/div nil
               (dom/h1 nil (str "Weather in " (:name weather)))
               (dom/h2 nil (str (get-in weather [:main :temp] " °C"))
                       (dom/img  #js {:src (str "http://openweathermap.org/img/w/" (:icon current-weather) ".png")}))
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
    [:new-city '(:weather {:city ?city})])
  Object
  (render [this]
    (let [{:keys [new-city weather]} (om/props this)
          city (-> this om/get-params :city)
          data (:data weather)
          error (:error weather)]
      (println "Data: " data)
      (println "Error: " error)
      (dom/div #js {:className "ApplicationView"}
               (dom/h1 nil "Current-Weather")
               (dom/input #js {:type     "text"
                               :focus    true
                               :value    new-city
                               :onChange #(om/transact! this `[(weather/set-new-city {:city ~(-> % .-target .-value)})])})
               (cc/button {:label    "Load"
                           :enabled  (pos? (count new-city))
                           :on-click #(do (om/set-query! this {:params {:city new-city}})
                                          (om/transact! this `[(weather/set-new-city {:city ~(-> % .-target .-value)})]))})
               (when data (weather-panel data))))))