(ns client.core
    (:require
     [client.components :refer [canvas game-layout game-score]]
     [client.helper-func :as h :refer [img-atom]]
     [reagent.dom :as rdom]))

(enable-console-print!)


(defn on-js-reload [])



(defn app []
  [:div {:class "game"} 
   [game-layout]
   [game-score]
   [canvas]
   (when-let [screenshot @img-atom]
     [:img {:src screenshot :alt "Game Region Screenshot" :class "screenshot-img"}])])
   

(rdom/render [app] (.getElementById js/document "app"))
