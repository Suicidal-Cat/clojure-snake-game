(ns client.core
    (:require
     [client.components :refer [canvas end-game-pop-up game-score profile]]
     [client.game :refer [connect_socket img-atom score start_game]]
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
                  (fn [] (connect_socket) (start_game) (swap! app-state assoc :show-game true))}
         "New game"]]]
      [profile]])
   (when (:show-game @app-state)
     (game-score score)) 
   [canvas]
   (when-let [screenshot @img-atom]
     [:img {:src screenshot :alt "Game Region Screenshot" :class "screenshot-img"}])])
   

(rdom/render [app] (.getElementById js/document "app"))
