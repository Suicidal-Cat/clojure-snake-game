(ns server.game
  (:require [ring.websocket :as ws]))

;hash-map - :snake array [X Y]
;(def game-state (atom {:snake1 [[100 100] [120 100] [140 100] [160 100] [180 100]]
;                       :snake2 [[300 120] [300 140] [300 160] [300 180] [300 200]]}))

;hash-map - :snake :player
(def snakes-direction (atom {:snake1 {}
                            :snake2 {}}))
(def stop-flag (atom false))

;update snake position
(defn move-snake [snake direction]
  (let [[x y] (first snake)]
    (case direction
      :up    (cons [x (- y 20)] (butlast snake))
      :down  (cons [x (+ y 20)] (butlast snake))
      :left  (cons [(- x 20) y] (butlast snake))
      :right (cons [(+ x 20) y] (butlast snake)))))

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

(defn broadcast-game-state [player1 player2]
  (future
    (reset! stop-flag false)
    (reset! snakes-direction {:snake1 (assoc player1 :direction :right)
                              :snake2 (assoc player2 :direction :right)})
    (let [game-state (atom {:snake1 [[180 100] [160 100] [140 100] [120 100] [100 100]]
                            :snake2 [[180 160] [160 160] [140 160] [120 160] [100 160]]})]
      (while (not @stop-flag)
        (Thread/sleep 200)
        (swap! game-state (fn [game-state] (hash-map
                                            :snake1 (move-snake (:snake1 game-state) (:direction (:snake1 @snakes-direction)))
                                            :snake2 (move-snake (:snake2 game-state) (:direction (:snake2 @snakes-direction))))))
        (ws/send (:socket player1) (pr-str @game-state))
        (ws/send (:socket player2) (pr-str @game-state))))))
    

(defn start-game [player1 player2]
  (broadcast-game-state player1 player2)
  )

(defn stop-game []
  (reset! stop-flag true))
