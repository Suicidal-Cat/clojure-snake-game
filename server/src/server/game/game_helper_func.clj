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
(defn generate-valid-coordinate-pair-ball [field-size grid-size sn1 sn2 & {:keys [offset] :or {offset (/ grid-size 2)}}]
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
        (mapv #(+ offset %) coordinate)))))

;init main game-state
(defn init-game-state [field-size grid-size]
  (let [snake1 [[162 108] [135 108] [108 108] [81 108]]
        snake2 [[405 486] [432 486] [459 486] [486 486]]
        ball (generate-valid-coordinate-pair-ball field-size grid-size
                                                  snake1
                                                  snake2)]
    (hash-map :snake1 snake1
              :snake2 snake2
              :ball ball
              :score [0 0])))

;init singleplayer game state
(defn game-state-single [field-size grid-size]
  (let [snake [[198 99] [165 99] [132 99] [99 99]]
        ball (generate-valid-coordinate-pair-ball field-size grid-size
                                                  snake
                                                  nil)]
    (hash-map :snake1 snake
              :ball ball
              :score [0])))

;init main game-state
(defn init-game-cake-state [field-size grid-size]
  (let [snake1 [[162 108] [135 108] [108 108] [81 108]]
        snake2 [[405 486] [432 486] [459 486] [486 486]]
        ball (generate-valid-coordinate-pair-ball field-size grid-size
                                                  snake1
                                                  snake2)]
    (hash-map :snake1 snake1
              :snake2 snake2
              :ball ball
              :score [0 0])))

;check if vector contains element
(defn vector-contains? [v el]
  (some #(= % el) v))

;find game by player's socket
(defn find-players-by-socket [socket online-games]
  (some (fn [[_ players]]
          (when (some #(= socket (:socket (val %))) @players)
            players))
        online-games))

;update snake position with border
(defn move-snake [snake direction speed]
  (let [[x y] (snake 0)
        new-head (case direction
                   :up    [x (- y speed)]
                   :down  [x (+ y speed)]
                   :left  [(- x speed) y]
                   :right [(+ x speed) y])]
    (into [new-head] (subvec snake 0 (dec (count snake))))))

;update snake position without border
(defn move-snake-borderless [snake direction speed field-size]
  (let [[x y] (snake 0)
        new-head (case direction
                   :up    [x (mod (- y speed) field-size)]
                   :down  [x (mod (+ y speed) field-size)]
                   :left  [(mod (- x speed) field-size) y]
                   :right [(mod (+ x speed) field-size) y])]
    (into [new-head] (subvec snake 0 (dec (count snake))))))