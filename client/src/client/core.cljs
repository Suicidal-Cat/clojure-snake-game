(ns client.core
    (:require
     [client.game :refer [connect_socket start_game]]
     [reagent.dom :as rdom]
     [reagent.core :as r]))

(enable-console-print!)


(defonce app-state (r/atom {:text "Hello world!"}))


(defn on-js-reload []
)



(defn canvas []
  [:div {:id "game-canvas"}])

(defn app []
  [:div {:class "game"}
   [:p "Snake game"]
   [:button {:class "start-game-btn"
             :on-click
             (fn [] (connect_socket) (start_game))}
    "Start Game"]
   [canvas]])

(rdom/render [app] (.getElementById js/document "app"))
