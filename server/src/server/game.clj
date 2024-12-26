(ns server.game
  (:require [ring.websocket :as ws]))

;hash-map - :snake array [X Y]
(def game-state (atom {:snake1 [[100 100] [120 100] [140 100] [160 100] [180 100]]
                       :snake2 [[300 120] [300 140] [300 160] [300 180] [300 200]]}))

;hash-map - :playerid :direction
(def snakes-direction (atom {:snake1 :right
                            :snake2 :left}))
;update snake position
(defn move-snake [snake direction]
  (let [[x y] (first snake)]
    (case direction
      :up    (cons [x (- y 20)] (butlast snake))
      :down  (cons [x (+ y 20)] (butlast snake))
      :left  (cons [(- x 20) y] (butlast snake))
      :right (cons [(+ x 20) y] (butlast snake)))))

;update snake direction
(defn change-direction [player dir]
  (let [past-dir (player @snakes-direction)]
    (if (not= past-dir dir)
      (cond
        (and (= past-dir :up) (not= dir :down)) (swap! snakes-direction update player (fn [_] dir))
        (and (= past-dir :down) (not= dir :up)) (swap! snakes-direction update player (fn [_] dir))
        (and (= past-dir :left) (not= dir :right)) (swap! snakes-direction update player (fn [_] dir))
        (and (= past-dir :right) (not= dir :left)) (swap! snakes-direction update player (fn [_] dir))))))



(def stop-flag (atom false))

(defn broadcast-game-state [socket]
  (future
    (reset! stop-flag false)
    (reset! game-state {:snake1 [[180 100] [160 100] [140 100] [120 100] [100 100]]
                        :snake2 [[300 120] [300 140] [300 160] [300 180] [300 200]]})
    (while (not @stop-flag)
      (Thread/sleep 100)
      (swap! game-state update :snake1 move-snake (:snake1 @snakes-direction))
      (ws/send socket (pr-str @game-state)))))

(defn stop-game []
  (reset! stop-flag true))