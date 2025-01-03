(ns server.game
  (:require
   [ring.websocket :as ws]
   [server.game-helper-func :refer [find-players-by-socket
                                    generate-valid-coordinate-pair-ball
                                    in-bounds? vector-contains?]]))

(def field-size 600) ;field size in px
(def grid-size 24) ;grid size in px
(def end-score 20) ;goal that player want to accomplish

;snakes-direction hash-map - (:snake1 plyer1 :snake2 player2)
;player - hash-map (:socket socket :direction direction)
;online-games - hash map :gameId snakes-direction
(def online-games (atom {}))

;stop the game and save the result
(defn end-game [stop-flag final-score result]
  (reset! final-score result)
  (reset! stop-flag true))

;update snake position
(defn move-snake [snake direction]
  (let [[x y] (first snake)
        new-head (case direction
                   :up    [x (- y grid-size)]
                   :down  [x (+ y grid-size)]
                   :left  [(- x grid-size) y]
                   :right [(+ x grid-size) y])]
    (into [new-head] (subvec snake 0 (dec (count snake))))))

;update snake direction
(defn change-direction [player-socket dir] 
  (let [snakes-direction (find-players-by-socket player-socket @online-games)
        [snake player] (if (= player-socket (:socket (:snake1 @snakes-direction)))
                         [:snake1 (:snake1 @snakes-direction)]
                         [:snake2 (:snake2 @snakes-direction)])
        past-dir (:direction player)
        update-dir (fn [] (swap! snakes-direction update snake (fn [_] (assoc player :direction dir))))]
    (if (not= past-dir dir)
      (cond
        (and (= past-dir :up) (not= dir :down)) (update-dir)
        (and (= past-dir :down) (not= dir :up)) (update-dir)
        (and (= past-dir :left) (not= dir :right)) (update-dir)
        (and (= past-dir :right) (not= dir :left)) (update-dir))
      nil)))

;check snake collisions
(defn snake-collisions [game-state stop-game final-score player1 player2]
  (let [snake1 (:snake1 @game-state)
        snake2 (:snake2 @game-state)]
    (if (or
         (false? (in-bounds? (first snake1) field-size grid-size))
         (vector-contains? snake2 (first snake1))
         (vector-contains? (subvec snake1 1) (first snake1))
         (= (last (:score @game-state)) end-score))
      (end-game stop-game final-score {:winner {:id (:id player2) :score (last (:score @game-state)) :head (first snake2)}
                                       :loser {:id (:id player1) :score (first (:score @game-state)) :head (first snake1)}})
      (if (or
           (false? (in-bounds? (first snake2) field-size grid-size))
           (vector-contains? snake1 (first snake2))
           (vector-contains? (subvec snake2 1) (first snake2))
           (= (first (:score @game-state)) end-score))
        (end-game stop-game final-score {:winner {:id (:id player1) :score (first (:score @game-state)) :head (first snake1)}
                                         :loser {:id (:id player2) :score (last (:score @game-state)) :head (first snake2)}})
        nil))))

;grow snake when it eats the ball and generate new ball
(defn update-game-on-eat [game-state]
  (let [[head-s1 & _] (:snake1 @game-state)
        [head-s2 & _] (:snake2 @game-state)
        fixed-ball (mapv #(- % (/ grid-size 2)) (:ball @game-state))]
    (if (= fixed-ball head-s1)
      (swap! game-state (fn [game-state] (hash-map :snake1 (conj (:snake1 game-state) [-1 -1])
                                                   :snake2 (:snake2 game-state)
                                                   :ball (generate-valid-coordinate-pair-ball field-size grid-size (:snake1 game-state) (:snake2 game-state))
                                                   :score [(inc (first (:score game-state))) (last (:score game-state))])))
      (if (= fixed-ball head-s2)
        (swap! game-state (fn [game-state] (hash-map :snake1 (:snake1 game-state)
                                                     :snake2 (conj (:snake2 game-state) [-1 -1])
                                                     :ball (generate-valid-coordinate-pair-ball field-size grid-size (:snake1 game-state) (:snake2 game-state))
                                                     :score [(first (:score game-state)) (inc (last (:score game-state)))])))
        nil))))

;game loop
(defn broadcast-game-state [player1 player2 game-id]
  (future
    (let [game-state (atom {:snake1 [[168 96] [144 96] [120 96] [96 96]]
                            :snake2 [[168 192] [144 192] [120 192] [96 192]]
                            :ball [300 300]
                            :score [0 0]})
          stop-game (atom false)
          final-score (atom nil)]
      (while (not @stop-game)
        (Thread/sleep 125)
        (ws/send (:socket player1) (pr-str @game-state))
        (ws/send (:socket player2) (pr-str @game-state))
        (update-game-on-eat game-state)
        (swap! game-state (fn [game-state] (hash-map
                                            ;:snake1 (move-snake (:snake1 game-state) (:direction (:snake1 (deref ((keyword game-id) @online-games)))))
                                            :snake1 (:snake1 game-state)
                                            :snake2 (move-snake (:snake2 game-state) (:direction (:snake2 (deref ((keyword game-id) @online-games)))))
                                            :ball (:ball game-state)
                                            :score (:score game-state))))
        (snake-collisions game-state stop-game final-score player1 player2))
      (Thread/sleep 50)
      (ws/send (:socket player1) (pr-str @final-score))
      (ws/send (:socket player2) (pr-str @final-score))
      (ws/close (:socket player1))
      (ws/close (:socket player2)))))
    
;start the game
(defn start-game [player1 player2]
  (let [game-id (str (:id player1) (:id player2))
        snakes-direction (atom {:snake1 (assoc player1 :direction :right)
                                :snake2 (assoc player2 :direction :right)})]
    (swap! online-games assoc (keyword game-id) snakes-direction)
    (broadcast-game-state player1 player2 game-id)))
