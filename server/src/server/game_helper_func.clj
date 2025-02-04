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
;may need update so its not in front of the snake
(defn generate-valid-coordinate-pair-ball [field-size grid-size snake1 snake2]
  (loop []
    (let [x (random-coordinate field-size grid-size)
          y (random-coordinate field-size grid-size)
          coordinate [x y]]
      (if (or (some #(= % coordinate) snake1)
              (some #(= % coordinate) snake2))
        (recur)
        (mapv #(+ (/ grid-size 2) %) coordinate)))))

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

;;25 main game
;; game-state (atom {:snake1 [[200 100] [175 100] [150 100] [125 100]]
;;                   :snake2 [[200 200] [175 200] [150 200] [125 200]]
;;                   :ball (generate-valid-coordinate-pair-ball field-size grid-size
;;                                                              [[200 100] [175 100] [150 100] [125 100]]
;;                                                              [[200 200] [175 200] [150 200] [125 200]])
;;                   :score [0 0]})

;; 27 594
;; (hash-map :snake1 [[168 96] [144 96] [120 96] [96 96]]
;;           :snake2 [[432 504] [456 504] [480 504] [504 504]]
;;           :ball ball
;;           :score [0 0])