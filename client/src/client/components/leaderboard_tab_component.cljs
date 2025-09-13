(ns client.components.leaderboard-tab-component 
  (:require
   [client.api.api-calls :refer [get-leaderboard]]
   [client.components.shared-components :refer [spinner]]
   [client.helper-func :refer [user-info]]
   [reagent.core :as r]))

(defonce leaderboard-tab-state {:leaderboard (r/atom nil)})

(defn get-leaderboard-table [isFriends]
  (get-leaderboard (:id @user-info) isFriends #(reset! (:leaderboard leaderboard-tab-state) %)))

(defn leaderboard []
  (if (:leaderboard leaderboard-tab-state)
    (let [leaderboard @(:leaderboard leaderboard-tab-state)
          current-user-id (:id @user-info)
          top10 (->> leaderboard
                     (filter #(= "Top10" (:resulttype %)))
                     (take 10))
          target (first (filter #(and (= "Target" (:resulttype %))
                                      (> (:rankpos %) 10))
                                leaderboard))
          rows (if target
                 (concat top10 [target])
                 top10)]
      [:<>
       [:table {:class "leaderboard-table"}
        [:thead
         [:tr
          [:th "Rank"]
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
              [:td (str (:winpercentage entry) "%")]]))]]
       [:div {:class "ldb-buttons"}
        [:button {:on-click #(get-leaderboard-table 0)} "GLOBAL"]
        [:button {:on-click #(get-leaderboard-table 1)} "LOCAL"]]]
      )
    [spinner]))