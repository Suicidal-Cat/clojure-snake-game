(ns server.game.game-helper-func)

;helper functions

;check if snake part is in playing field
(defn in-bounds? [[x y] field-size grid-size]
  (and (>= x grid-size)
       (< x (- field-size grid-size))
       (>= y grid-size)
       (< y (- field-size grid-size))))

;check if coordinates are inside given field
(defn inside? [[x y] x-rec y-rec h-rec w-rec]
  (and (>= x x-rec)
       (< x (+ x-rec w-rec))
       (>= y y-rec)
       (< y (+ y-rec h-rec))))

;generate random cordinates in playing field
(defn random-coordinate [field-size grid-size]
  (let [num-cells (/ (- field-size (* 2 grid-size)) grid-size)]
    (+ grid-size (* grid-size (rand-int num-cells)))))

;generate valid coordinate pair
(defn generate-valid-coordinate-pair-ball [field-size grid-size sn1 sn2 & {:keys [offset] :or {offset 0}}]
  (loop []
    (let [x (random-coordinate field-size grid-size)
          y (random-coordinate field-size grid-size)
          coordinate [x y]
          safe-area (* grid-size 2)]
      (if (or (some #(= % coordinate) sn1)
              (some #(= % coordinate) sn2)
              (inside? coordinate (- ((sn1 0) 0) safe-area) (- ((sn1 0) 1) safe-area) (* safe-area 2) (* safe-area 2))
              (if sn2 (inside? coordinate (- ((sn2 0) 0) safe-area) (- ((sn2 0) 1) safe-area) (* safe-area 2) (* safe-area 2)) false))
        (recur)
        (mapv #(+ (/ grid-size 2) offset %) coordinate)))))

;init main game-state
(defn init-game-state [field-size grid-size]
  (let [ball (generate-valid-coordinate-pair-ball field-size grid-size
                                                  [[162 108] [135 108] [108 108] [81 108]]
                                                  [[405 486] [432 486] [459 486] [486 486]])]
    (hash-map :snake1 [[162 108] [135 108] [108 108] [81 108]]
              :snake2 [[405 486] [432 486] [459 486] [486 486]]
              :ball ball
              :score [0 0])))

;init singleplayer game state
(defn game-state-single [field-size grid-size]
  (let [ball (generate-valid-coordinate-pair-ball field-size grid-size
                                                  [[180 90] [150 90] [120 90] [90 90]]
                                                  nil)]
    (hash-map :snake1 [[180 90] [150 90] [120 90] [90 90]]
              :ball ball
              :score [0])))

;check if vector contains element
(defn vector-contains? [v el]
  (some #(= % el) v))

;find game by player's socket
(defn find-players-by-socket [socket online-games]
  (some (fn [[_ players]]
          (when (some #(= socket (:socket (val %))) @players)
            players))
        online-games))