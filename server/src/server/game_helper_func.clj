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
