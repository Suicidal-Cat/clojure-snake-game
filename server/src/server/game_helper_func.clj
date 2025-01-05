(ns server.game-helper-func)

;helper functions

;check if snake part is in playing field
(defn in-bounds? [[x y] field-size grid-size]
  (and (>= x grid-size)
       (< x (- field-size grid-size))
       (>= y grid-size)
       (< y (- field-size grid-size))))

;generate random cordinates in playing field
(defn random-coordinate [field-size grid-size]
  (let [num-cells (/ (- field-size (* 2 grid-size)) grid-size)]
    (+ grid-size (* grid-size (rand-int num-cells)))))

;generate valid coordinate pair
(defn generate-valid-coordinate-pair-ball [field-size grid-size snake1 snake2]
  (loop []
    (let [x (random-coordinate field-size grid-size)
          y (random-coordinate field-size grid-size)
          coordinate [x y]]
      (if (or (some #(= % coordinate) snake1)
              (some #(= % coordinate) snake2))
        (recur)
        (mapv #(+ (/ grid-size 2) %) coordinate)))))

;check if vector contains element
(defn vector-contains? [v element]
  (some #(= % element) v))

;find game by player's socket
(defn find-players-by-socket [socket online-games]
  (some (fn [[_ players]]
          (when (some #(= socket (:socket (val %))) @players)
            players))
        online-games))

(defn generate-random-power [game-state stop-game power-ups field-size grid-size]
  (future 
    (while (not @stop-game)
      (Thread/sleep (+ 6000 (rand-int 3001)))
      (let [power (rand-nth power-ups)
            cordinates (generate-valid-coordinate-pair-ball field-size grid-size (:snake1 game-state) (:snake2 game-state))
            duration 3500]
        (swap! game-state (fn [game-state] (assoc game-state :power {:value power :cord cordinates})))
        (Thread/sleep duration)
        (swap! game-state (fn [game-state] (assoc game-state :power nil)))))))