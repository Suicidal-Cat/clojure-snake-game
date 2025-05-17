(ns client.core
  (:require
   [client.components :refer [canvas game-layout game-score login-form
                              register-form screenshoot-canvas]]
   [reagent.dom :as rdom]))

(enable-console-print!)

(defn on-js-reload [])

(defn app []
  [:div {:class "game"}
   [game-layout]
   [game-score]
   [canvas]
   [screenshoot-canvas]
   [login-form]
   [register-form]])


(rdom/render [app] (.getElementById js/document "app"))
