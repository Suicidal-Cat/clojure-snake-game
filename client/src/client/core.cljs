(ns client.core
    (:require
     [client.components :refer [canvas game-layout game-score login-form
                                screenshoot-canvas]]
     [reagent.dom :as rdom]))

(enable-console-print!)

(defn on-js-reload [])

(defn app []
  [:div {:class "game"}
   [login-form]
   [game-layout]
   [game-score]
   [canvas]
   [screenshoot-canvas]])
   

(rdom/render [app] (.getElementById js/document "app"))
