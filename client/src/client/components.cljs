(ns client.components
  (:require
   [client.api.api-calls :refer [login]]
   [client.helper-func :as h :refer [img-atom]]
   [client.main-game :as main]
   [client.singleplayer-game :as single]
   [reagent.core :as r]))

(defonce app-state (r/atom {:show-game false}))

(defn canvas []
  [:div {:id "game-canvas"}])

(defn game-score []
  (when (:show-game @app-state)
    (let [score main/score]
      [:div {:class "score"}
       [:div {:class "score1"} (first @score)]
       [:div {:class "score2"} (last @score)]])))

(defn profile []
  [:img {:src "/images/profile.png"
         :alt "snake profile"
         :class "snake-profile"}])

(defn screenshoot-canvas []
  (when-let [screenshot @img-atom]
    [:img {:src screenshot :alt "Game Region Screenshot" :class "screenshot-img"}]))

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

(defn login-form []
  (let [message (r/atom "")]
    [:div
     [:h2 "Login"]
     [:form {:on-submit (fn [e]
                          (.preventDefault e)
                          (let [form-data (js/FormData. (.-target e))
                                username (.get form-data "username")
                                password (.get form-data "password")]
                            (login username password (fn [result] (println result)))))}
      [:div
       [:label "Username: "]
       [:input {:type "text" :name "username"}]]
      [:div
       [:label "Password: "]
       [:input {:type "password" :name "password"}]]
      [:button {:type "submit"} "Login"]]
     [:p @message]]))

