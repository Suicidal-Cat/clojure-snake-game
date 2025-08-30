(ns client.game.main-game
  (:require
   [client.game.game-helper-func :refer [draw-snake random-snake-images]]
   [client.helper-func :as hf :refer [format-time game-mode-enum get-player-id
                                      save-region-screenshot! show-end-dialog]]
   [clojure.edn :as edn]
   [quil.core :as q]
   [reagent.core :as r]))


(def score (r/atom [0 0]))
(def game-time (r/atom "2:00"))
(def game-state (r/atom nil))
(def field-size 594) ;field size in px
(def grid-size 27) ;grid size in px
(def circle-radius 24); radius for draw generation
(def end-score-data (r/atom nil))
(def loading-flag (atom false))
(def stop-game-flag (atom false))
(def player-id (atom 0))


;stoping game
(defn stop-game [data]
  (let [winner (:winner data)
        [hx hy] (mapv #(* % 2) (:head winner))]
    (reset! end-score-data data)
    (save-region-screenshot! (max 0 (- hx 180)) (max 0 (- hy 180)) 400 400)
    (reset! stop-game-flag true)
    (reset! show-end-dialog true)))

;canvas setup
(defn setup []
  (let [bomb  "/images/bomb.png"
        add3  "/images/+3.png"
        minus3  "/images/-3.png"]
    (q/set-state!
     :radius 0
     :bomb (q/load-image bomb)
     :add3 (q/load-image add3)
     :minus3 (q/load-image minus3)))
  (doseq [[k v] (random-snake-images)]
    (swap! (q/state-atom) assoc k v))
  (q/frame-rate 30)
  (q/background 0))

;; pulse animation
(defn pulse [min max]
  (let [min-radius min
        max-radius max
        t         (/ (+ 1 (Math/sin (/ (q/frame-count) 15.0))) 2)
        radius    (+ min-radius (* t (- max-radius min-radius)))]
    (swap! (q/state-atom) assoc :radius radius)))

;draw edges and grid
(defn draw-grid-border [grid-size]
  (q/fill 148 148 148)
  (q/rect 0 0 (q/width) grid-size)
  (q/rect 0 0 grid-size (q/height))
  (q/rect 0 (- (q/height) grid-size) (q/width) grid-size)
  (q/rect (- (q/width) grid-size) 0 grid-size (q/height))
  (q/stroke 50)
  (q/stroke-weight 1)
  (q/fill 0 0 0)
  (doseq [x (range grid-size (- (q/width) grid-size) grid-size)]
    (q/line x 0 x (q/height)))
  (doseq [y (range grid-size (- (q/height) grid-size) grid-size)]
    (q/line 0 y (q/width) y)))

;draw food
(defn draw-food []
  (q/fill 0 0 255)
  (q/ellipse (first (:ball @game-state)) (last (:ball @game-state)) circle-radius circle-radius))

;draw power
(defn draw-power []
  (when-let [power (:power @game-state)]
    (let [r (q/state :radius)
          x (first (:cord power))
          y (last (:cord power))]
      (if (:random power)
        (do (q/fill 255 255 0) (q/ellipse x y circle-radius circle-radius))
        (do
          (pulse 27 45)
          (case (:value power)
            "+3" (q/image (q/state :add3) (- x (/ grid-size 2)) (- y (/ grid-size 2)) grid-size grid-size)
            "-3" (q/image (q/state :minus3) (- x (/ grid-size 2)) (- y (/ grid-size 2)) grid-size grid-size)
            "boom" (q/image (q/state :bomb) (- x (/ r 2)) (- y (/ r 2)) r r)
            nil))))))

;draw snakes
(defn draw-snakes []
  (draw-snake (q/state :head1) (q/state :body1) (:snake1 @game-state) grid-size)
  (draw-snake (q/state :head2) (q/state :body2) (:snake2 @game-state) grid-size))

;stop drawing
(defn stop-drawing []
  (when @stop-game-flag (q/no-loop)))

;main draw
(defn draw []
  (q/background 0)
  (draw-grid-border grid-size)
  (draw-food)
  (draw-power)
  (draw-snakes)
  (stop-drawing))

;start the game
(defn start-game []
  (q/sketch
   :host "game-canvas"
   :settings #(q/smooth 2)
   :setup setup
   :draw draw
   :size [field-size field-size]))

;websocket communication
(defn connect_socket [disable-loading] 
  (reset! loading-flag false)
  (let [ws (js/WebSocket. "ws://localhost:8085/ws")
        handle-keypress (fn handle-keypress [e]
                          (let [key (.-key e)]
                            (case key
                              "ArrowUp" (.send ws {:direction :up :time true})
                              "ArrowDown" (.send ws {:direction :down :time true})
                              "ArrowLeft" (.send ws {:direction :left :time true})
                              "ArrowRight" (.send ws {:direction :right :time true}))))]
    (.addEventListener js/document "keydown" handle-keypress)
    (.addEventListener ws "open" (fn [_]
                                   (reset! stop-game-flag false)
                                   (let [id (get-player-id)]
                                     (reset! player-id id)
                                     (.send ws {:id id :game-mode (:time game-mode-enum)}))))
    (.addEventListener ws "message" (fn [e]
                                      (let [data (edn/read-string (.-data e))]
                                        (reset! game-state data)
                                        (reset! score (:score data))
                                        (reset! game-time (format-time (:time-left data))) 
                                        (when (:winner data) (stop-game data)))

                                      (when (not @loading-flag)
                                        (disable-loading)
                                        (reset! end-score-data nil)
                                        (reset! loading-flag true))))
    (.addEventListener ws "close" (fn [_] (println "WebSocket closed")
                                    (.removeEventListener js/document "keydown" handle-keypress)))))



