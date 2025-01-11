(ns client.core
    (:require
     [client.components :refer [canvas game-score profile]]
     [client.main-game :as main :refer [img-atom]]
     [client.singleplayer-game :as single]
     [reagent.core :as r]
     [reagent.dom :as rdom]))

(enable-console-print!)


(defonce app-state (r/atom {:show-game false}))


(defn on-js-reload [])



(defn app []
  [:div {:class "game"} 
   (when-not (:show-game @app-state)
     [:div {:class "game-cont"}
      [:div {:class "start-game"}
       [:img {:src "/images/snake-logo.png"
              :alt "snake logo"
              :class "snake-logo"}]
       [:div {:class "menu-buttons"}
        [:button {:class "start-game-btn"
                  :on-click
                  (fn [] (main/connect_socket) (main/start_game) (swap! app-state assoc :show-game true))}
         "MULTIPLAYER"]
        [:button {:class "start-game-btn"
                  :on-click
                  (fn [] (single/connect_socket) (single/start_game) (swap! app-state assoc :show-game true))}
         "SINGLEPLAYER"]]]
      [profile]])
   (when (:show-game @app-state)
     (game-score main/score)) 
   [canvas]
   (when-let [screenshot @img-atom]
     [:img {:src screenshot :alt "Game Region Screenshot" :class "screenshot-img"}])])
   

(rdom/render [app] (.getElementById js/document "app"))
