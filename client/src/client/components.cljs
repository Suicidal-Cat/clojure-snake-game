(ns client.components
  (:require
   [client.api.api-calls :refer [login register]]
   [client.helper-func :as h :refer [get-user-info img-atom set-local-storage]]
   [client.main-game :as main]
   [client.singleplayer-game :as single]
   [reagent.core :as r]))

(defonce app-state (r/atom {:show-game false
                            :logged false
                            :show-register false}))

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
  (swap! app-state assoc :logged (some? (get-user-info)))
  (when (not (:logged @app-state)) (let [message (r/atom "")]
                                     [:div
                                      [:h2 "Login"]
                                      [:form {:on-submit (fn [e]
                                                           (.preventDefault e)
                                                           (let [form-data (js/FormData. (.-target e))
                                                                 email (.get form-data "email")
                                                                 password (.get form-data "password")]
                                                             (login email password (fn [result] (if (:id result)
                                                                                                  (do (set-local-storage "user" result)
                                                                                                      (swap! app-state assoc :logged true))
                                                                                                  (swap! app-state assoc :logged false))))))}
                                       [:div
                                        [:label "Email: "]
                                        [:input {:type "email" :name "email" :required true}]]
                                       [:div
                                        [:label "Password: "]
                                        [:input {:type "password" :name "password" :required true :min-length 8}]]
                                       [:button {:type "submit"} "Login"]]
                                      [:p @message]])))

(defn register-form []
  (when (and (:show-register @app-state) (not (:logged @app-state)))
             (let [message (r/atom "")]
               [:div
                [:h2 "Register"]
                [:form {:on-submit (fn [e]
                                     (.preventDefault e)
                                     (let [form-data (js/FormData. (.-target e))
                                           email (.get form-data "email")
                                           username (.get form-data "username")
                                           password (.get form-data "password")]
                                       (register email username password (fn [result] (println result)))))}
                 [:div
                  [:label "Email: "]
                  [:input {:type "email" :name "email" :required true}]]
                 [:div
                  [:label "Username: "]
                  [:input {:type "text" :name "username" :required true :min-length 4}]]
                 [:div
                  [:label "Password: "]
                  [:input {:type "password" :name "password" :required true :min-length 8}]]
                 [:button {:type "submit"} "Register"]]
                [:p @message]])))

