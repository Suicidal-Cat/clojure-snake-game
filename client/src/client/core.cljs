(ns client.core
  (:require
   [client.components.main-component :refer [canvas game-layout game-score screenshoot-canvas]]
   [reagent.dom :as rdom]))

(enable-console-print!)

(defn on-js-reload [])

(defn app []
  [:div {:class "game"}
   [game-layout]
   [game-score]
   [canvas]
   [screenshoot-canvas]])


(rdom/render [app] (.getElementById js/document "app"))
