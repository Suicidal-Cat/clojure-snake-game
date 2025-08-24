(ns client.core
  (:require
   [client.components.main-component :refer [border-terrain canvas
                                             end-game-pop-up game-score
                                             home-layout]]
   [reagent.dom :as rdom]))

(enable-console-print!)

(defn on-js-reload [])

(defn app []
  [:div {:class "game"}
   [home-layout]
   [canvas]
   [border-terrain]
   [game-score]
   [end-game-pop-up]])


(rdom/render [app] (.getElementById js/document "app"))
