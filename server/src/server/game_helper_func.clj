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

;init main game-state
(defn init-game-state [field-size grid-size]
  (let [ball (generate-valid-coordinate-pair-ball field-size grid-size
                                                  [[168 96] [144 96] [120 96] [96 96]]
                                                  [[432 504] [456 504] [480 504] [504 504]])]
    (hash-map :snake1 [[168 96] [144 96] [120 96] [96 96]]
              :snake2 [[432 504] [456 504] [480 504] [504 504]]
              :ball ball
              :score [0 0])))

;init singleplayer game state
(defn game-state-single [field-size grid-size]
  (let [ball (generate-valid-coordinate-pair-ball field-size grid-size
                                                  [[180 90] [150 90] [120 90] [90 90]]
                                                  nil)]
    (hash-map :snake1 [[180 90] [150 90] [120 90] [90 90]]
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

;update game-state based on power
(defn update-power-consumed [game-state sn-consum sn-opp power-val]
  (let [sn-consum-v (sn-consum game-state)
        sn-consum-size (count sn-consum-v)]
    (case power-val
      "+3" (assoc game-state sn-opp (conj (sn-opp game-state) [-1 -1] [-1 -1] [-1 -1]) :power nil)
      "-3" (if (> sn-consum-size 5) (assoc game-state sn-consum (subvec sn-consum-v 0 (- sn-consum-size 3)) :power nil) game-state))))


;;25 main game
;; game-state (atom {:snake1 [[200 100] [175 100] [150 100] [125 100]]
;;                   :snake2 [[200 200] [175 200] [150 200] [125 200]]
;;                   :ball (generate-valid-coordinate-pair-ball field-size grid-size
;;                                                              [[200 100] [175 100] [150 100] [125 100]]
;;                                                              [[200 200] [175 200] [150 200] [125 200]])
;;                   :score [0 0]})