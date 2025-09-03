(ns client.components.main-component
  (:require
   [client.components.friends-tab-component :refer [friend-request
                                                    get-available-requests
                                                    get-pending-requests]]
   [client.components.leaderboard-tab-component :refer [get-leaderboard-table
                                                        leaderboard]]
   [client.components.match-history-tab-component :refer [get-matches
                                                          match-history]]
   [client.components.sign-in-tabs-component :refer [login-form register-form]]
   [client.game.cake-game :as cake]
   [client.game.main-game :as main]
   [client.game.singleplayer-game :as single]
   [client.helper-func :as h :refer [clear-local-storage game-mode-enum
                                     get-user-info img-atom show-end-dialog]]
   [reagent.core :as r]))

(defonce app-state (r/atom {:show-game false
                            :show-loading false
                            :show-tabs false
                            :logged false
                            :active-tab (r/atom 1)
                            :game-mode nil}))
;; check if user is logged
(defn update-user-state []
  (let [user (get-user-info)] 
    (swap! app-state assoc :logged (some? user))))

(update-user-state)

(defn canvas []
  (r/create-class
   {:reagent-render
    (fn []
      [:div {:id "game-canvas"}])

    :component-did-mount
    (fn []
      (let [mode (:game-mode @app-state)]
        (cond
          (= mode "single") (single/start-game)
          (= mode (:time game-mode-enum)) (main/start-game)
          (= mode (:cake game-mode-enum)) (cake/start-game))))

    :component-did-update
    (fn [this old-argv]
      (let [mode (:game-mode @app-state)]
        (cond
          (= mode "single") (single/start-game)
          (= mode (:time game-mode-enum)) (main/start-game)
          (= mode (:cake game-mode-enum)) (cake/start-game))))}))

;; terrain around canvas based on mode
(defn border-terrain []
  (when (:show-game @app-state)
    (let [mode (:game-mode @app-state)]
      (cond
        (= mode (:time game-mode-enum)) [:img {:src "/images/grass-terrain.png"
                                 :class "grass-terrain-single"}]))))
;; loading screen
(defn loading []
  [:div {:class "load-cont"}
   [:div {:class "loading-box"}
    [:div
     [:span {:class "letter l1"} "L"]
     [:span {:class "letter l2"} "o"]
     [:span {:class "letter l3"} "a"]
     [:span {:class "letter l4"} "d"]
     [:span {:class "letter l5"} "i"]
     [:span {:class "letter l6"} "n"]
     [:span {:class "letter l7"} "g"]]
    [:div
     [:span {:class "letter dot"} "."]
     [:span {:class "letter dot"} "."]
     [:span {:class "letter dot"} "."]]]])

;; game score component for cake mode
(defn game-score-cake []
  (let [game-state @cake/game-state]
    [:div {:class "cake-score-cont"}
     [:div {:class "cake-score"}
      (for [part (get-in game-state [:cake1 :parts])]
        ^{:key (:part-id part)}
        [:div {:class "part"}
         [:img {:src (str "/images/parts/" (:part-image part))
                :alt (:part-image part)
                :class "part-image"}]
         [:div {:class "part-text"}
          (str (:current part) "/" (:amount part))]])]
     [:div {:class "cake-score"}
      (for [part (get-in game-state [:cake2 :parts])]
        ^{:key (:part-id part)}
        [:div {:class "part"}
         [:img {:src (str "/images/parts/" (:part-image part))
                :alt (:part-image part)
                :class "part-image"}]
         [:div {:class "part-text"}
          (str (:current part) "/" (:amount part))]])]]))

;; game score based on mode
(defn game-score []
  (when (:show-game @app-state) 
    (let [mode (:game-mode @app-state)]
      (cond
        (and (= mode (:cake game-mode-enum)) (not= nil @cake/game-state) (= false @cake/stop-game-flag))
        (game-score-cake)
        
        (= mode (:time game-mode-enum))
        (let [score main/score]
          [:div {:class "score"}
           [:div {:class "score1"} (first @score)]
           [:div @main/game-time]
           [:div {:class "score2"} (last @score)]])
        
        (= mode "single") ""))))

;; profile icon
(defn profile []
  [:img {:src "/images/profile.png"
         :alt "snake profile"
         :class "snake-profile"
         :on-click #(swap! app-state update :show-tabs not)}])

;; logout icon
(defn logout []
  (when (and (:logged @app-state) (not (:show-tabs @app-state)))
    [:img {:src "/images/logout.png"
           :alt "logout"
           :class "logout"
           :on-click #(do
                        (clear-local-storage)
                        (swap! app-state assoc :logged false))}]))

;; screenshoot of the canvas
(defn screenshoot-canvas []
  (when-let [screenshot @img-atom]
    [:img {:src screenshot :alt "Game Region Screenshot" :class "screenshot-img"}]))

;; dialog that shows at the end of the game
(defn end-game-pop-up []
  (when @show-end-dialog
    (let [mode (:game-mode @app-state)]
    [:div {:class "end-game-dialog"}
     [screenshoot-canvas]
     [:div {:class "end-score"}
      (cond
        (= mode "single") (str "Score: " (get-in @single/end-score-data [:winner :score]))
        (= mode (:time game-mode-enum)) (if (some? (:draw @main/end-score-data))
                                          "DRAW"
                                          [:<>
                                           [:div (if (= @main/player-id (get-in @main/end-score-data [:winner :id]))
                                                   "Victory" "Defeat")]
                                           [:div (str (get-in @main/end-score-data [:winner :score]) " - " (get-in @main/end-score-data [:loser :score]))]])
        (= mode (:cake game-mode-enum))
        (let [cake (if (= @cake/player-id (get-in @cake/end-score-data [:winner :id]))
                     (get-in @cake/end-score-data [:winner :cake]) (get-in @cake/end-score-data [:loser :cake]))]
          [:<>
           [:div (if (= @cake/player-id (get-in @cake/end-score-data [:winner :id]))
                   "Victory" "Defeat")]
           [:div {:class "end-cake-score-cont"} 
            [:div {:class "end-cake-score"}
             (for [part (cake :parts)]
               ^{:key (:part-id part)}
               [:div {:class "end-part"}
                [:img {:src (str "/images/parts/" (:part-image part))
                       :alt (:part-image part)
                       :class "end-part-image"}]
                [:div {:class "end-part-text"}
                 (str (:current part) "/" (:amount part))]])]]]))] 
     
     [:button {:class "end-button"
               :on-click
               (fn []
                 (swap! app-state assoc :show-game false)
                 (reset! show-end-dialog false)
                 (reset! img-atom nil))}
      "CONTINUE"]])))

;; header tab
(defn tab-header [tab-image index class]
  [:div {:class "tab" 
         :on-click #(swap! (:active-tab @app-state) (fn [_] index))}
   [:img {:src tab-image :class (str "tab-image" class (if (= @(:active-tab @app-state) index) " hovered" ""))}]])

;; show content based on clicked tab
(defn tab-content []
  (case @(:active-tab @app-state)
    1 (if (:logged @app-state)
        (do (get-leaderboard-table)
            [leaderboard])
        [login-form #(swap! app-state assoc :logged true)])
    2 (if (:logged @app-state)  
        (do (get-matches)
            [match-history]) 
        [register-form #(reset! (:active-tab @app-state) 1)])
    3 (do (get-available-requests nil)
          (get-pending-requests)
          [friend-request])))

;; list tabs
(defn user-dialog []
  [:div {:class "user-dialog"}
   (if (:logged @app-state)
     [:div {:class "tabs"}
      [tab-header "images/leaderboard.png" 1 ""]
      [tab-header "images/history.png" 2 " hs-img"]
      [tab-header "images/add_friend.png" 3 ""]]
     [:div {:class "tabs"}
      [tab-header "Login" 1]
      [tab-header "Register" 2]])
   [tab-content]])

;; main screen
(defn home-layout []
  (when-not (:show-game @app-state)
      [:div {:class "game-cont"} 
       (if (:show-tabs @app-state)
         [user-dialog]
         [:div {:class "start-game"}
          [:img {:src "/images/snake-logo.png"
                 :alt "snake logo"
                 :class "snake-logo"}]
          [:div {:class "menu-buttons"}
           [:button {:class "start-game-btn"
                     :on-click
                     (fn []
                       (main/connect_socket #(swap! app-state assoc :show-game true :show-loading false))
                       (swap! app-state assoc :show-game false :show-loading true :game-mode (:time game-mode-enum)))}
            "TIME GAME"]
           [:button {:class "start-game-btn"
                     :on-click
                     (fn []
                       (cake/connect_socket #(swap! app-state assoc :show-game true :show-loading false))
                       (swap! app-state assoc :show-game false :show-loading true :game-mode (:cake game-mode-enum)))}
            "CAKE GAME"]
           [:button {:class "start-game-btn"
                     :on-click
                     (fn [] 
                       (single/connect_socket #(swap! app-state assoc :show-game true :show-loading false))
                       (swap! app-state assoc :show-game true :game-mode "single"))}
            "SINGLEPLAYER"]]])
       
       (when (:show-loading @app-state) [loading])
       [profile]
       [logout]]))

