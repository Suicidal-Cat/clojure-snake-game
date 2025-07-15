(ns client.components.main-component
  (:require
   [client.api.api-calls :refer [login register]]
   [client.components.friends-tab-component :refer [friend-request
                                                    get-available-requests
                                                    get-pending-requests]]
   [client.components.leaderboard-tab-component :refer [get-leaderboard-table
                                                        leaderboard]]
   [client.components.match-history-tab-component :refer [get-matches
                                                          match-history]]
   [client.helper-func :as h :refer [get-user-info img-atom set-local-storage]]
   [client.main-game :as main :refer [game-time]]
   [client.singleplayer-game :as single]
   [reagent.core :as r]))

(defonce app-state (r/atom {:show-game false
                            :logged false
                            :active-tab (r/atom 1)}))

(defn update-user-state []
  (let [user (get-user-info)]
    (if (some? user)
      (swap! app-state assoc :logged true :id (:id user))
      (swap! app-state assoc :logged false))))

(update-user-state)

(defn canvas []
  [:div {:id "game-canvas"}])

(defn game-score []
  (when (:show-game @app-state)
    (let [score main/score]
      [:div {:class "score"}
       [:div {:class "score1"} (first @score)]
       [:div @game-time]
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
                                                                                                      (update-user-state))
                                                                                                  (update-user-state))))))}
                                       [:div
                                        [:label "Email: "]
                                        [:input {:type "email" :name "email" :required true}]]
                                       [:div
                                        [:label "Password: "]
                                        [:input {:type "password" :name "password" :required true :min-length 8}]]
                                       [:button {:type "submit"} "Login"]]
                                      [:p @message]])))

(defn register-form []
  (when (not (:logged @app-state))
    (let [message (r/atom "")]
      [:div
       [:h2 "Register"]
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (let [form-data (js/FormData. (.-target e))
                                  email (.get form-data "email")
                                  username (.get form-data "username")
                                  password (.get form-data "password")
                                  form-el (.-target e)]
                              (register email username password
                                        (fn [result] (when result
                                                       (reset! (:active-tab @app-state) 1)
                                                       (.reset form-el)
                                                       (reset! message ""))))))}
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

(defn tab-header [label index]
  [:div {:class "tab"
         :style {:border-bottom (when (= @(:active-tab @app-state) index) "2px solid blue")}
         :on-click #(swap! (:active-tab @app-state) (fn [_] index))}
   label])

(defn tab-content []
  (case @(:active-tab @app-state)
    1 (if (:logged @app-state)
        (do (get-leaderboard-table)
            [leaderboard])
        [login-form])
    2 (if (:logged @app-state)  
        (do (get-matches)
            [match-history]) 
        [register-form])
    3 (do (get-available-requests nil)
          (get-pending-requests)
          [friend-request])))

(defn user-dialog []
  [:div {:class "user-dialog"}
   (if (:logged @app-state)
     [:<>
      [tab-header "Leaderboard" 1]
      [tab-header "Match History" 2]
      [tab-header "Friend requests" 3]]
     [:<>
      [tab-header "Login" 1]
      [tab-header "Register" 2]])
   [tab-content]])

