(ns client.game.main-game
  (:require
   [client.game.game-helper-func :refer [draw-grid-standard
                                         draw-player-indicator draw-snake
                                         get-food-image pulse-normal
                                         random-snake-images]]
   [client.helper-func :as hf :refer [api-domain format-time game-mode-enum
                                      get-player-id save-region-screenshot!]]
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
        [hx hy] (:head winner)]
    (reset! end-score-data data) 
    (save-region-screenshot! (max 0 (- hx 100)) (max 0 (- hy 100)) (* 2 (min 340 (- field-size (- hx 340)))) (* 2 (min 340 (- field-size (- hx 340)))))
    (reset! stop-game-flag true)))

;canvas setup
(defn setup []
  (let [bomb  "/images/bomb.png"
        add3  "/images/+3.png"
        minus3  "/images/-3.png"
        random-img "/images/question.png"
        indicator "/images/indicator.png"
        food-image (get-food-image)]
    (q/set-state!
     :radius 0
     :radius-norm 0
     :bomb (q/load-image bomb)
     :add3 (q/load-image add3)
     :minus3 (q/load-image minus3)
     :random-img (q/load-image random-img)
     :indicator (q/load-image indicator)
     :cactus (q/load-image "/images/cactus.png")
     :food-img (q/load-image (str "/images/parts/" food-image))
     :ind-y 0))
  (doseq [[k v] (random-snake-images)]
    (swap! (q/state-atom) assoc k v))
  (q/frame-rate 30)
  (q/background 0))

;; pulse animation for the bomb
(defn pulse [min max]
  (let [min-radius min
        max-radius max
        t         (/ (+ 1 (Math/sin (/ (q/frame-count) 15.0))) 2)
        radius    (+ min-radius (* t (- max-radius min-radius)))]
    (swap! (q/state-atom) assoc :radius radius)))

;draw food
(defn draw-food []
  (when-let [[x y] (:ball @game-state)]
    (let [r (q/state :radius-norm)]
      (q/image-mode :center)
      (q/image (q/state :food-img) x y r r)
      (q/image-mode :corner))))

;draw power
(defn draw-power []
  (when-let [power (:power @game-state)]
    (let [r (q/state :radius)
          x (first (:cord power))
          y (last (:cord power))]
      (if (:random power)
        (q/image (q/state :random-img) (- x (/ grid-size 2)) (- y (/ grid-size 2)) grid-size grid-size)
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
  (draw-grid-standard grid-size (q/state :cactus))
  (swap! (q/state-atom) assoc :radius-norm (pulse-normal 27 32))
  (draw-food)
  (draw-power)
  (draw-snakes) 
  (draw-player-indicator "indicator" game-state player-id)
  (stop-drawing))

;start the game
(defn start-game [] 
  (reset! stop-game-flag false)
  (q/sketch
   :host "game-canvas"
   :settings #(q/smooth 2)
   :setup setup
   :draw draw
   :size [field-size field-size]))

;websocket communication
(defn connect_socket [disable-loading] 
  (reset! loading-flag false)
  (let [ws (js/WebSocket. (str "ws://" api-domain "/ws"))
        handle-keypress (fn handle-keypress [e]
                          (let [key (.-key e)]
                            (case key
                              "ArrowUp" (.send ws {:direction :up :time true})
                              "ArrowDown" (.send ws {:direction :down :time true})
                              "ArrowLeft" (.send ws {:direction :left :time true})
                              "ArrowRight" (.send ws {:direction :right :time true}))))]
    (.addEventListener js/document "keydown" handle-keypress)
    (.addEventListener ws "open" (fn [_]
                                   (let [id (get-player-id)]
                                     (reset! player-id id)
                                     (.send ws {:id id :game-mode (:time game-mode-enum)}))))
    (.addEventListener ws "message" (fn [e]
                                      (let [data (edn/read-string (.-data e))] 
                                        (if (:winner data)
                                          (do
                                            (reset! stop-game-flag true)
                                            (js/setTimeout #(stop-game data) 250))
                                          (do
                                            (reset! game-state data)
                                            (reset! score (:score data))
                                            (reset! game-time (format-time (:time-left data))))))

                                      (when (not @loading-flag)
                                        (disable-loading)
                                        (reset! end-score-data nil)
                                        (reset! loading-flag true))))
    (.addEventListener ws "close" (fn [_] (println "WebSocket closed")
                                    (.removeEventListener js/document "keydown" handle-keypress)))))



