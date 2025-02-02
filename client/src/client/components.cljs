(ns client.components
  (:require [client.main-game :as main]
            [client.singleplayer-game :as single]
            [reagent.core :as r]))

(defonce app-state (r/atom {:show-game false}))

(defn canvas []
  [:div {:id "game-canvas"}])

(defn game-score [score]
  (when (:show-game @app-state)
    (let [score main/score]
      [:div {:class "score"}
       [:div {:class "score1"} (first @score)]
       [:div {:class "score2"} (last @score)]])))

(defn profile []
  [:img {:src "/images/profile.png"
         :alt "snake profile"
         :class "snake-profile"}])

(defn end-game-pop-up []
  [:div {:class "end-game-dialog"}])

(defn game-layout []
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
     [profile]]))

