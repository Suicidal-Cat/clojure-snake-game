(ns client.components
  (:require
   [client.api.api-calls :refer [get-leaderboard get-match-history login
                                 register]]
   [client.helper-func :as h :refer [get-user-info img-atom set-local-storage]]
   [client.main-game :as main]
   [client.singleplayer-game :as single]
   [reagent.core :as r]))

(defonce app-state (r/atom {:show-game false
                            :logged false
                            :active-tab (r/atom 1)
                            :match-history (r/atom nil)
                            :leaderboard (r/atom nil)}))

(defn update-user-state []
  (let [user (get-user-info)]
    (if (some? user)
      (swap! app-state assoc :logged true :id (:id user))
      (swap! app-state assoc :logged false))))

(update-user-state)

(defn spinner []
  [:div {:class "spinner"}])

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

(defn leaderboard []
  (when (nil? @(:leaderboard @app-state))
    (get-leaderboard (:id @app-state) #(reset! (:leaderboard @app-state) %)))
  (if (:leaderboard @app-state)
    (let [leaderboard @(:leaderboard @app-state)
          current-user-id (:id @app-state)
          top10 (->> leaderboard
                     (filter #(= "Top10" (:resulttype %)))
                     (take 10))
          target (first (filter #(and (= "Target" (:resulttype %))
                                      (> (:rankpos %) 10))
                                leaderboard))
          rows (if target
                 (concat top10 [target])
                 top10)]
      [:table {:class "leaderboard-table"}
       [:thead
        [:tr
         [:th "Rank & Username"]
         [:th "Games Played"]
         [:th "Games Won"]
         [:th "Win %"]]]
       [:tbody
        (for [entry rows]
          (let [highlight? (= (:userid entry) current-user-id)
                row-class (if highlight? "highlight-target" "")]
            ^{:key (str (:userid entry) "-" (:resulttype entry))}
            [:tr {:class row-class}
             [:td (str (:rankpos entry) ". " (:username entry))]
             [:td (:gamesplayed entry)]
             [:td (:gameswon entry)]
             [:td (str (:winpercentage entry) "%")]]))]])
    [spinner]))

(defn match-history []
  (when (nil? @(:match-history @app-state))
    (get-match-history (:id @app-state) #(reset! (:match-history @app-state) %)))
  (if (:match-history @app-state)
    (let [matches @(:match-history @app-state)]
      [:div {:class "match-container" }
       (for [match matches]
          (let [result-color (case (:result match)
                                         "Won"  "#d4edda"
                                         "Lost" "#f8d7da"
                                         "#e2e3e5")]
            ^{:key (:id match)}
            [:div {:class "match-card"
                   :style {:background-color result-color}}
             [:div {:class "match-opponent"} (str "vs " (:opponent match))]
             [:div {:class "match-score"} (:score match)]
             [:div {:class "match-time"} (:played_ago match)]]))])
    [spinner]))

(defn tab-header [label index]
  [:div {:class "tab"
         :style {:border-bottom (when (= @(:active-tab @app-state) index) "2px solid blue")}
         :on-click #(swap! (:active-tab @app-state) (fn [_] index))}
   label])

(defn tab-content []
  (case @(:active-tab @app-state)
    1 (if (:logged @app-state) [leaderboard] [login-form]) 
    2 (if (:logged @app-state)  [match-history] [register-form])))

(defn user-dialog []
  [:div {:class "user-dialog"}
   (if (:logged @app-state)
     [:<>
      [tab-header "Leaderboard" 1]
      [tab-header "Match History" 2]]
     [:<>
      [tab-header "Login" 1]
      [tab-header "Register" 2]])
   [tab-content]])

