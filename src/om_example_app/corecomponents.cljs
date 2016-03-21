(ns om-example-app.corecomponents
  (:require [reagent.core :as reagent]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))


(defn check-label [label checked]
  [:div {:class (if checked "CheckLabel-checked" "CheckLabel-unchecked")} label])

(defn check-label-list [checks]
  [:div.CheckLabelList
   (for [{:keys [label checked]} checks]
     ^{:key label} [check-label label checked])])

(defui Button
  Object
  (render [this]
    (let [{:keys [label enabled on-click] :or {:enabled true}} (om/props this)]
      (dom/button #js {:disabled (not enabled)
                      :onClick (when enabled on-click)} label))))

(def button (om/factory Button))


#_(defn button [label & {:keys [enabled on-click] :or {enabled true}}]
  [:button {:disabled (not enabled) :on-click (when enabled on-click)} label])

(defn button-bar [& children]
  (into [:div.ButtonBar] children))

(defn message-dialog [message button-title on-click]
  [:div message
   [button-bar
    [button button-title :on-click on-click]]])

(defui NavigationBar
  Object
  (render [this]
    (let [{:keys [views current-route on-click]} (om/props this)
          view-pairs (partition 2 views)]
      (apply dom/ul #js {:className "NavigationBar"}
             (for [[route label] view-pairs]
               (dom/li #js {:className (if (= route current-route)
                                         "NavigationBar-Item NavigationBar-Item-Active"
                                         "NavigationBar-Item")
                            :onClick #(on-click route)} label))))))


(def navigation-bar (om/factory NavigationBar))

#_(defn navigation-bar [views active-view on-click]
    {:pre [(even? (count views))]}
    (let [view-pairs (partition 2 views)]
      [:ul.NavigationBar
       (for [[label view] view-pairs]
         [:li {:key      view
               :class    (if (= view active-view)
                           "NavigationBar-Item NavigationBar-Item-Active"
                           "NavigationBar-Item")
               :on-click #(on-click view)} label])]))

(def initial-focus-wrapper
  (with-meta identity
             {:component-did-mount #(.focus (reagent/dom-node %))}))

