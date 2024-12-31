(ns client.core
    (:require
     [client.game :refer [connect_socket start_game score]]
     [reagent.dom :as rdom]
     [reagent.core :as r]))

(enable-console-print!)


(defonce app-state (r/atom {:show-game false}))


(defn on-js-reload [])



(defn canvas []
  [:div {:id "game-canvas"}])

(defn app []
  [:div {:class "game"}
   (when-not (:show-game @app-state)
     [:div {:class "start-game"}
      [:p "Snake game"]
      [:button {:class "start-game-btn"
                :on-click
                (fn [] (connect_socket) (start_game) (swap! app-state assoc :show-game true))}
       "Start Game"]])
   (when (:show-game @app-state)
     [:div {:class "score"}
      [:div {:class "score1"} (first @score)]
      [:div {:class "score2"} (last @score)]])
     [canvas]])
   

(rdom/render [app] (.getElementById js/document "app"))
