(ns client.components.match-history-tab-component 
  (:require
   [client.api.api-calls :refer [get-match-history]]
   [client.components.shared-components :refer [spinner]]
   [client.helper-func :refer [user-info]]
   [reagent.core :as r]))


(defonce match-tab-state {:match-history (r/atom nil)})

(defn get-matches []
  (get-match-history (:id @user-info) #(reset! (:match-history match-tab-state) %)))

(defn match-history []
  (if (:match-history match-tab-state)
    (let [matches @(:match-history match-tab-state)]
      [:div {:class "match-container"}
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