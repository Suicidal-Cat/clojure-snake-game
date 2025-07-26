(ns server.game.main-game
  (:require
   [ring.websocket :as ws]
   [server.game.game-helper-func :refer [find-players-by-socket
                                    generate-valid-coordinate-pair-ball
                                    in-bounds? init-game-state inside?
                                    vector-contains?]]
   [server.db.dbBroker :as db]))

(def field-size 594) ;field size in px
(def grid-size 27) ;grid size in px
(def end-score 20) ;goal that player want to accomplish
(def power-ups ["+3","-3","boom"])
(def tick-duration 120)

;snakes-direction hash-map - (:snake1 plyer1 :snake2 player2)
;player - hash-map (:socket socket :direction direction)
;online-games - hash map :gameId snakes-direction
(def online-games (atom {}))

;stop the game and save the result
(defn end-game-loop [stop-flag final-score result]
  (reset! final-score result)
  (reset! stop-flag true)
  (db/save-game @final-score true (:time db/game-mode-enum)))

;update snake position
(defn move-snake [snake direction speed]
  (let [[x y] (snake 0)
        new-head (case direction
                   :up    [x (- y speed)]
                   :down  [x (+ y speed)]
                   :left  [(- x speed) y]
                   :right [(+ x speed) y])]
    (into [new-head] (subvec snake 0 (dec (count snake))))))

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
         (false? (in-bounds? (snake1 0) field-size grid-size))
         (vector-contains? snake2 (snake1 0))
         (vector-contains? (subvec snake1 1) (snake1 0))
         (>= ((:score @game-state) 1) end-score))
      (end-game-loop stop-game final-score {:winner {:id (:id player2) :score ((:score @game-state) 1) :head (snake2 0)}
                                            :loser {:id (:id player1) :score ((:score @game-state) 0) :head (snake1 0)}})
      (when (or
             (false? (in-bounds? (snake2 0) field-size grid-size))
             (vector-contains? snake1 (snake2 0))
             (vector-contains? (subvec snake2 1) (snake2 0))
             (>= ((:score @game-state) 0) end-score))
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

;generate power on the field
(defn generate-random-power [game-state stop-game power-ups field-size grid-size]
  (future
    (while (not @stop-game)
      (Thread/sleep (+ 6000 (rand-int 3001)))
      (let [power (rand-nth power-ups)
            cordinates (generate-valid-coordinate-pair-ball field-size grid-size (:snake1 @game-state) (:snake2 @game-state))
            duration 5000
            hehe (if (and (not= power "boom") (= (rand-int (count power-ups)) 1)) true false)]
        (if hehe
          (swap! game-state (fn [game-state] (assoc game-state :power {:value power :cord cordinates :random true})))
          (swap! game-state (fn [game-state] (assoc game-state :power {:value power :cord cordinates}))))
        (Thread/sleep duration)
        (if (= power "boom")
          (swap! game-state (fn [game-state] (assoc-in game-state [:power, :value] "boomed")))
          (swap! game-state (fn [game-state] (assoc game-state :power nil))))))))

;update game-state based on power
(defn update-power-consumed [game-state sn-consum sn-opp power-val]
  (let [sn-consum-v (sn-consum game-state)
        sn-consum-size (count sn-consum-v)]
    (case power-val
      "+3" (assoc game-state sn-opp (conj (sn-opp game-state) [-1 -1] [-1 -1] [-1 -1]) :power nil)
      "-3" (if (> sn-consum-size 5) (assoc game-state sn-consum (subvec sn-consum-v 0 (- sn-consum-size 3)) :power nil) game-state)
      "boom" (assoc game-state :lost sn-consum))))

;update game on boomed
(defn update-game-boomed [game-state final-score stop-game player1 player2 snh1 snh2 cord]
  (let [x (- (cord 0) (* grid-size 3/2))
        y (- (cord 1) (* grid-size 3/2))
        size (* grid-size 3)
        i1 (inside? snh1 x y size size)
        i2 (inside? snh2 x y size size)]
    (if (and i1 i2)
      (end-game-loop stop-game final-score {:winner {:id (:id player2) :score ((:score @game-state) 1) :head snh2}
                                            :loser {:id (:id player1) :score ((:score @game-state) 0) :head snh1}
                                            :draw true})
      (if i1 (end-game-loop stop-game final-score {:winner {:id (:id player2) :score ((:score @game-state) 1) :head snh2}
                                                   :loser {:id (:id player1) :score ((:score @game-state) 0) :head snh1}})
          (if i2 (end-game-loop stop-game final-score {:winner {:id (:id player1) :score ((:score @game-state) 0) :head snh1}
                                                         :loser {:id (:id player2) :score ((:score @game-state) 1) :head snh2}})
              (swap! game-state (fn [game-state] (assoc game-state :power nil))))))))

;update game on consumed power
(defn update-game-on-power [game-state final-score stop-game player1 player2]
  (when-let [power (:power @game-state)]
    (let [[head-s1 & _] (:snake1 @game-state)
          [head-s2 & _] (:snake2 @game-state)
          power-cord (mapv #(- % (/ grid-size 2)) (:cord power))]
      (if (= (:value power) "boomed")
        (update-game-boomed game-state final-score stop-game player1 player2 head-s1 head-s2 power-cord)
        (if (= power-cord head-s1)
          (do (swap! game-state (fn [game-state] (update-power-consumed game-state :snake1 :snake2 (:value power))))
              (when (:lost game-state)
                (end-game-loop stop-game final-score {:winner {:id (:id player2) :score ((:score @game-state) 1) :head head-s2}
                                                      :loser {:id (:id player1) :score ((:score @game-state) 0) :head head-s1}})))
          (when (= power-cord head-s2)
            (swap! game-state (fn [game-state] (update-power-consumed game-state :snake2 :snake1 (:value power))))
            (when (:lost @game-state)
              (end-game-loop stop-game final-score {:winner {:id (:id player1) :score ((:score @game-state) 0) :head head-s1}
                                                    :loser {:id (:id player2) :score ((:score @game-state) 1) :head head-s2}}))))))))

;update snakes positions
(defn update-snakes-positions [game-state snake-directions]
  (assoc game-state
         :snake1 (:snake1 game-state)
         :snake2 (move-snake (:snake2 game-state) (:direction (:snake2 @snake-directions)) grid-size)))

(defn update-clock-time [game-state final-score stop-game player1 player2]
  (swap! game-state update :time-left #(max 0 (- % tick-duration)))
  (when (zero? (:time-left @game-state))
    (let [[score1 score2] (:score @game-state)
          [head-s1 & _] (:snake1 @game-state)
          [head-s2 & _] (:snake2 @game-state)]
      (cond
        (> score1 score2) (end-game-loop stop-game final-score {:winner {:id (:id player1) :score ((:score @game-state) 0) :head head-s1}
                                                                :loser {:id (:id player2) :score ((:score @game-state) 1) :head head-s2}})
        (< score1 score2) (end-game-loop stop-game final-score {:winner {:id (:id player2) :score ((:score @game-state) 1) :head head-s2}
                                                                :loser {:id (:id player1) :score ((:score @game-state) 0) :head head-s1}})
        :else             (end-game-loop stop-game final-score {:winner {:id (:id player2) :score ((:score @game-state) 1) :head head-s1}
                                                                :loser {:id (:id player1) :score ((:score @game-state) 0) :head head-s2}
 
                                                                :draw true})))))
;; send snake data
(defn send-snake-data [player game-state]
  (ws/send (:socket player) (pr-str game-state)))

;game loop
(defn broadcast-game-state [player1 player2 game-id]
  (future
    (let [game-state (atom (assoc (init-game-state field-size grid-size)
                                  :time-left (* 2 60 1000)))
          snake-directions ((keyword game-id) @online-games)
          stop-game (atom false)
          final-score (atom nil)] 
      (send-snake-data player1 @game-state)
      (send-snake-data player2 @game-state)
      (Thread/sleep 3000)
      (generate-random-power game-state stop-game power-ups field-size grid-size)
      (while (not @stop-game)
        (Thread/sleep tick-duration)
        (send-snake-data player1 @game-state)
        (send-snake-data player2 @game-state)
        (update-game-on-eat game-state)
        (update-game-on-power game-state final-score stop-game player1 player2)
        (swap! game-state update-snakes-positions snake-directions)
        (snake-collisions game-state stop-game final-score player1 player2)
        (update-clock-time game-state final-score stop-game player1 player2)
        (swap! snake-directions (fn [state] (assoc-in (assoc-in state [:snake1 :change-dir] true) [:snake2 :change-dir] true))))
      (Thread/sleep 50)
      (ws/send (:socket player1) (pr-str @final-score))
      (ws/send (:socket player2) (pr-str @final-score))
      (ws/close (:socket player1))
      (ws/close (:socket player2)))))

;start the game
(defn start-game [player1 player2]
  (let [game-id (str (:id player1) (:id player2))
        snakes-direction (atom {:snake1 (assoc player1 :direction :right :change-dir true)
                                :snake2 (assoc player2 :direction :left :change-dir true)})]
    (swap! online-games assoc (keyword game-id) snakes-direction)
    (broadcast-game-state player1 player2 game-id)))