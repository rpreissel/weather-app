(ns weather-app.reframe
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-handler
                                   register-sub
                                   dispatch
                                   dispatch-sync
                                   subscribe]]
            [reagent.core :as reagent]
            [goog.string :as gstring]
            [goog.string.format :as gformat]
            [goog.dom :as gdom]
            [ajax.core :refer [GET]]))

(declare set-city)
(declare fetch-weather)

(defn city-input-panel [city]
  [:div
   [:h1 "Current Weather"]
   [:input {:type      "text" :focus true :value city
            :on-change #(set-city (-> % .-target .-value))}]
   [:button {:on-click #(fetch-weather city)
             :disabled (-> city count pos? not)} "Load"]])

(defn weather-panel [weather]
  (println "render weather panel")
  (if-not weather
    [:h1 "No weather data available"]
    (let [current-weather (get-in weather [:weather 0])]
      [:div
       [:h1 (str "Weather in " (:name weather))]
       [:h2 (str (get-in weather [:main :temp] " °C"))
        [:img {:src (str "http://openweathermap.org/img/w/" (:icon current-weather) ".png")}]]
       [:p (:description current-weather)]])))


(defn weather-view []
  (let [city (subscribe [:city])
        weather (subscribe [:weather])]
    (fn []
      (println "render weather view")
      (let [{:keys [data error]} @weather]
        [:div.ApplicationView
         [city-input-panel @city]
         (when data [weather-panel data])
         (when error [:div.Red (str "Error: " error)])]))))


(defn init []
  (let [city (subscribe [:city])]
    (when-not @city
      (dispatch-sync [:init])
      (fetch-weather @city))))

(defn set-city [city]
  (dispatch [:set-city city]))

(def api-key "444112d540b141913a9c1ee6d7f495fa")
(defn fetch-weather [city]
  (println "fetch: " city)
  (GET (gstring/format "http://api.openweathermap.org/data/2.5/weather?q=%s,de&appid=%s&units=metric" city api-key)
       {:handler         #(do
                           (println "Success: " %)
                           (dispatch [:set-weather % nil]))
        :error-handler   #(do
                           (println "Error: " %)
                           (dispatch [:set-weather nil (:status-text %)]))
        :response-format :json
        :keywords?       true}))


(register-handler
  :init
  (fn [_ _]
    {:city    "Hamburg"
     :weather {}}))

(register-handler
  :set-city
  (fn [db [_ city]]
    (assoc db :city city)))

(register-handler
  :set-weather
  (fn [db [_ data error]]
    (-> db
        (assoc-in [:weather :data] data)
        (assoc-in [:weather :error] error))))

(register-sub
  :city
  (fn [db]
    (reaction
      (:city @db))))

(register-sub
  :weather
  (fn [db]
    (reaction
      (:weather @db))))

(defn render-component [elem]
  (init)
  (reagent/render-component [weather-view]
                            (gdom/getElement elem)))