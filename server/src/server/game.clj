(ns server.game
  (:require
   [ring.websocket :as ws]
   [server.game-helper-func :refer [in-bounds? random-coordinate]]))

;hash-map - :snake array [X Y]
(def game-state (atom {:snake1 [[180 100] [160 100] [140 100] [120 100] [100 100]]
                       :snake2 [[180 160] [160 160] [140 160] [120 160] [100 160]]
                       :ball [290 290]}))

;hash-map - :snake :player
(def snakes-direction (atom {:snake1 {}
                            :snake2 {}}))

(def stop-flag (atom false))

(defn stop-game []
  (reset! stop-flag true))


;update snake position
(defn move-snake [snake direction]
  (let [[x y] (first snake)
        new-head (case direction
                   :up    [x (- y 20)]
                   :down  [x (+ y 20)]
                   :left  [(- x 20) y]
                   :right [(+ x 20) y])]
    (into [new-head] (subvec snake 0 (dec (count snake))))))

;update snake direction
(defn change-direction [player-socket dir]
  (let [[snake player] (if (= player-socket (:socket (:snake1 @snakes-direction))) 
                         [:snake1 (:snake1 @snakes-direction)] 
                         [:snake2 (:snake2 @snakes-direction)])
        past-dir (:direction player)]
    (if (not= past-dir dir)
      (cond
        (and (= past-dir :up) (not= dir :down)) (swap! snakes-direction update snake (fn [_] (assoc player :direction dir)))
        (and (= past-dir :down) (not= dir :up)) (swap! snakes-direction update snake (fn [_] (assoc player :direction dir)))
        (and (= past-dir :left) (not= dir :right)) (swap! snakes-direction update snake (fn [_] (assoc player :direction dir)))
        (and (= past-dir :right) (not= dir :left)) (swap! snakes-direction update snake (fn [_] (assoc player :direction dir)))))))

;check snake collisions
(defn snake-collisions []
  (let [[head-s1 & _] (:snake1 @game-state)
       [head-s2 & _] (:snake2 @game-state)]
    (if (false? (in-bounds? head-s1 600 20))
      (stop-game)
      (if (false? (in-bounds? head-s2 600 20))
        (stop-game)
        ()))))

;grow snake when it eats the ball and generate new ball
(defn update-game-on-eat []
  (let [[head-s1 & _] (:snake1 @game-state)
        [head-s2 & _] (:snake2 @game-state)
        ball (:ball @game-state)
        fixed-ball (mapv #(- % 10) ball)]
    (if (= fixed-ball head-s1)
      (swap! game-state (fn [game-state] (hash-map :snake1 (conj (:snake1 game-state) [-1 -1])
                                                   :snake2 (:snake2 game-state)
                                                   :ball [(random-coordinate 600 20) (random-coordinate 600 20)])))
      (if (= fixed-ball head-s2)
        (fn [game-state] (hash-map :snake1 (:snake1 game-state)
                                   :snake2 (conj (:snake2 game-state) [-1 -1])
                                   :ball [(random-coordinate 600 20) (random-coordinate 600 20)]))
        ()))))

(defn broadcast-game-state [player1 player2]
  (future
    (reset! stop-flag false)
    (reset! snakes-direction {:snake1 (assoc player1 :direction :right)
                              :snake2 (assoc player2 :direction :right)})
    (reset! game-state {:snake1 [[180 100] [160 100] [140 100] [120 100] [100 100]]
                        :snake2 [[180 160] [160 160] [140 160] [120 160] [100 160]]
                        :ball [290 290]})
    (while (not @stop-flag)
      (Thread/sleep 150)
      (ws/send (:socket player1) (pr-str @game-state))
      (ws/send (:socket player2) (pr-str @game-state))
      (update-game-on-eat)
      (swap! game-state (fn [game-state] (hash-map
                                          :snake1 (move-snake (:snake1 game-state) (:direction (:snake1 @snakes-direction)))
                                          ;:snake2 (move-snake (:snake2 game-state) (:direction (:snake2 @snakes-direction)))
                                          :snake2 (:snake2 game-state)
                                          :ball (:ball game-state))))
      (snake-collisions))))
    

(defn start-game [player1 player2]
  (broadcast-game-state player1 player2)
  )
