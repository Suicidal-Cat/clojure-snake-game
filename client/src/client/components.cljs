(ns client.components)

(defn canvas []
  [:div {:id "game-canvas"}])

(defn game-score [score]
  [:div {:class "score"}
   [:div {:class "score1"} (first @score)]
   [:div {:class "score2"} (last @score)]])

