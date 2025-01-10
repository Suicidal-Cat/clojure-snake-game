(ns server.singleplayer-game
  (:require
   [ring.websocket :as ws]
   [server.game-helper-func :refer [find-players-by-socket game-state-single]]))

(def field-size 600) ;field size in px
(def grid-size 30) ;grid size in px

;snakes-direction hash-map - (:snake1 plyer1)
;player - hash-map (:socket socket :direction direction)
;online-games - hash map :gameId snakes-direction
(def online-games (atom {}))

(defn change-direction-single [player-socket dir]
  (let [snakes-direction (find-players-by-socket player-socket @online-games)
        [snake player] [:snake1 (:snake1 @snakes-direction)]
        past-dir (:direction player)
        update-dir (fn [] (swap! snakes-direction update snake (fn [_] (assoc player :direction dir :change-dir false))))]
    (when (:change-dir (snake @snakes-direction))
      (if (not= past-dir dir)
        (cond
          (and (= past-dir :up) (not= dir :down)) (update-dir)
          (and (= past-dir :down) (not= dir :up)) (update-dir)
          (and (= past-dir :left) (not= dir :right)) (update-dir)
          (and (= past-dir :right) (not= dir :left)) (update-dir))
        nil))))

;update snake position
(defn move-snake [snake direction speed field-size]
  (let [[x y] (snake 0)
        new-head (case direction
                   :up    [x (mod (- y speed) field-size)]
                   :down  [x (mod (+ y speed) field-size)]
                   :left  [(mod (- x speed) field-size) y]
                   :right [(mod (+ x speed) field-size) y])]
    (into [new-head] (subvec snake 0 (dec (count snake))))))

(defn broadcast-game-state [player1 game-id]
  (future
    (let [game-state (atom (game-state-single field-size grid-size))
          snake-directions ((keyword game-id) @online-games)
          stop-game (atom false)
          final-score (atom nil)]
      (while (not @stop-game)
        (Thread/sleep 120)
        (ws/send (:socket player1) (pr-str @game-state))
        ;(update-game-on-eat game-state)
        (swap! game-state (fn [game-state]
                            (assoc game-state
                                   :snake1 (move-snake (:snake1 game-state) (:direction (:snake1 @snake-directions)) grid-size field-size))))
        ;(snake-collisions game-state stop-game final-score player1)
        (swap! snake-directions (fn [state] (assoc-in state [:snake1 :change-dir] true)))) 
      (Thread/sleep 50)
      (ws/send (:socket player1) (pr-str @final-score))
      (ws/close (:socket player1)))))

;start the game
(defn start-game-single [player1]
  (let [game-id (str (:id player1))
        snakes-direction (atom {:snake1 (assoc player1 :direction :right :change-dir true)})]
    (swap! online-games assoc (keyword game-id) snakes-direction)
    (broadcast-game-state player1 game-id)))