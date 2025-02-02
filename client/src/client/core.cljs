(ns client.core
    (:require
     [client.components :refer [canvas game-score page-layout
                                screenshoot-canvas]]
     [reagent.dom :as rdom]))

(enable-console-print!)

(defn on-js-reload [])

(defn app []
  [:div {:class "game"} 
   [page-layout]
   [game-score]
   [canvas]
   [screenshoot-canvas]])
   

(rdom/render [app] (.getElementById js/document "app"))
