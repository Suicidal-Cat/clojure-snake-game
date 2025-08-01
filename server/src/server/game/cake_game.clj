(ns server.game.cake-game
  (:require
   [ring.websocket :as ws]
   [server.db.dbBroker :as db :refer [get-random-cake-with-parts]]
   [server.game.game-helper-func :refer [find-players-by-socket
                                         generate-valid-coordinate-pair-ball
                                         init-game-cake-state
                                         move-snake-borderless
                                         vector-contains?]]))

(def field-size 594) ;field size in px
(def grid-size 27) ;grid size in px
(def tick-duration 120)

;snakes-direction hash-map - (:snake1 plyer1 :snake2 player2)
;player - hash-map (:socket socket :direction direction)
;online-games - hash map :gameId snakes-direction
(def online-games (atom {}))

;stop the game and save the result
(defn end-game-loop [stop-flag final-score result]
  (reset! final-score result)
  (reset! stop-flag true)
  (db/save-game @final-score true (:cake db/game-mode-enum)))

;update snake direction
(defn change-direction [player-socket dir]
  (let [snakes-direction (find-players-by-socket player-socket @online-games)
        [snake player] (if (= player-socket (:socket (:snake1 @snakes-direction)))
                         [:snake1 (:snake1 @snakes-direction)]
                         [:snake2 (:snake2 @snakes-direction)])
        past-dir (:direction player)
        update-dir (fn [] (swap! snakes-direction update snake (fn [_] (assoc player :direction dir :change-dir false))))]
    (when (and (:change-dir (snake @snakes-direction)) (not= past-dir dir))
      (cond
        (and (= past-dir :up) (not= dir :down)) (update-dir)
        (and (= past-dir :down) (not= dir :up)) (update-dir)
        (and (= past-dir :left) (not= dir :right)) (update-dir)
        (and (= past-dir :right) (not= dir :left)) (update-dir)))))

;check snake collisions
(defn snake-collisions [game-state stop-game final-score player1 player2]
  (let [snake1 (:snake1 @game-state)
        snake2 (:snake2 @game-state)]
    (if (or
         (vector-contains? snake2 (snake1 0))
         (vector-contains? (subvec snake1 1) (snake1 0)))
      (end-game-loop stop-game final-score {:winner {:id (:id player2) :score ((:score @game-state) 1) :head (snake2 0)}
                                            :loser {:id (:id player1) :score ((:score @game-state) 0) :head (snake1 0)}})
      (when (or
             (vector-contains? snake1 (snake2 0))
             (vector-contains? (subvec snake2 1) (snake2 0)))
        (end-game-loop stop-game final-score {:winner {:id (:id player1) :score ((:score @game-state) 0) :head (snake1 0)}
                                              :loser {:id (:id player2) :score ((:score @game-state) 1) :head (snake2 0)}})))))

;grow snake when it eats the ball and generate new ball
(defn update-game-on-eat [game-state]
  (let [[head-s1 & _] (:snake1 @game-state)
        [head-s2 & _] (:snake2 @game-state)
        fixed-ball (mapv #(- % (/ grid-size 2)) (:ball @game-state))]
    (if (= fixed-ball head-s1)
      (swap! game-state (fn [game-state] (assoc game-state
                                                :snake1 (conj (:snake1 game-state) [-1 -1])
                                                :ball (generate-valid-coordinate-pair-ball field-size grid-size (:snake1 game-state) (:snake2 game-state))
                                                :score [(inc ((:score game-state) 0)) ((:score game-state) 1)])))
      (when (= fixed-ball head-s2)
        (swap! game-state (fn [game-state] (assoc game-state
                                                  :snake2 (conj (:snake2 game-state) [-1 -1])
                                                  :ball (generate-valid-coordinate-pair-ball field-size grid-size (:snake1 game-state) (:snake2 game-state))
                                                  :score [((:score game-state) 0) (inc ((:score game-state) 1))])))))))

;update snakes positions
(defn update-snakes-positions [game-state snake-directions]
  (assoc game-state
         :snake1 (:snake1 game-state)
         :snake2 (move-snake-borderless (:snake2 game-state) (:direction (:snake2 @snake-directions)) grid-size field-size)))

;; send snake data
(defn send-snake-data [player game-state]
  (ws/send (:socket player) (pr-str game-state)))

;game loop
(defn broadcast-game-state [player1 player2 game-id]
  (future
    (let [game-state (atom (assoc (init-game-cake-state field-size grid-size)
                                  :cake1 (get-random-cake-with-parts)
                                  :cake2 (get-random-cake-with-parts)))
          snake-directions ((keyword game-id) @online-games)
          stop-game (atom false)
          final-score (atom nil)]
      (send-snake-data player1 @game-state)
      (send-snake-data player2 @game-state)
      (Thread/sleep 3000)
      (while (not @stop-game)
        (Thread/sleep tick-duration)
        (send-snake-data player1 @game-state)
        (send-snake-data player2 @game-state)
        (update-game-on-eat game-state)
        (swap! game-state update-snakes-positions snake-directions)
        (snake-collisions game-state stop-game final-score player1 player2)
        (swap! snake-directions (fn [state] (assoc-in (assoc-in state [:snake1 :change-dir] true) [:snake2 :change-dir] true))))
      (Thread/sleep 50)
      (send-snake-data player1 @game-state)
      (send-snake-data player2 @game-state)
      (ws/close (:socket player1))
      (ws/close (:socket player2)))))

;start the game
(defn start-cake-game [player1 player2]
  (let [game-id (str (:id player1) (:id player2))
        snakes-direction (atom {:snake1 (assoc player1 :direction :right :change-dir true)
                                :snake2 (assoc player2 :direction :left :change-dir true)})]
    (swap! online-games assoc (keyword game-id) snakes-direction)
    (broadcast-game-state player1 player2 game-id)))