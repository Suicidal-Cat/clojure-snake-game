(ns client.game.cake-game
  (:require
   [client.game.game-helper-func :refer [draw-grid-standard
                                         draw-player-indicator draw-snake
                                         pulse-normal random-snake-images]]
   [client.helper-func :as hf :refer [api-domain game-mode-enum get-player-id
                                      save-region-screenshot!]]
   [clojure.edn :as edn]
   [quil.core :as q]
   [reagent.core :as r]))

(def game-state (r/atom nil))
(def field-size 594) ;field size in px
(def grid-size 27) ;grid size in px
(def circle-radius 24); radius for draw generation
(def loading-flag (atom false))
(def stop-game-flag (atom false))
(def end-score-data (r/atom nil))
(def player-id (atom 0))


;stoping game
(defn stop-game [data]
  (let [winner (:winner data)
        [hx hy] (:head winner)] 
    (save-region-screenshot! (max 0 (- hx 100)) (max 0 (- hy 100)) (* 2 (min 340 (- field-size (- hx 340)))) (* 2 (min 340 (- field-size (- hx 340)))))
    (reset! end-score-data data)
    (reset! stop-game-flag true)))

(defn load-cake-images []
  {:apple.png     (q/load-image "/images/parts/apple.png")
   :banana.png    (q/load-image "/images/parts/banana.png")
   :butter.png    (q/load-image "/images/parts/butter.png")
   :cherry.png    (q/load-image "/images/parts/cherry.png")
   :floury.png    (q/load-image "/images/parts/floury.png")
   :grape.png     (q/load-image "/images/parts/grape.png")
   :honey.png     (q/load-image "/images/parts/honey.png")
   :lemon.png     (q/load-image "/images/parts/lemon.png")
   :milk.png      (q/load-image "/images/parts/milk.png")
   :strawberry.png (q/load-image "/images/parts/strawberry.png")})



(defn setup []
  (q/set-state! :radius 0)
  (doseq [[k v] (load-cake-images)]
    (swap! (q/state-atom) assoc k v))
  (doseq [[k v] (random-snake-images)]
    (swap! (q/state-atom) assoc k v))
  (swap! (q/state-atom) assoc :indicator (q/load-image "/images/indicator.png") :ind-y 0 :radius-norm 0)
  (q/frame-rate 30)
  (q/background 0))

;draw food
(defn draw-parts []
  (doseq [part (:parts @game-state)]
    (let [[x y] (:coordinate part)
          r (q/state :radius-norm)]
      (q/image-mode :center)
      (q/image (q/state (keyword (:image part))) (+ x (/ grid-size 2)) (+ y (/ grid-size 2)) r r)
      (q/image-mode :corner))))

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
  (draw-grid-standard grid-size) 
  (swap! (q/state-atom) assoc :radius-norm (pulse-normal 27 32))
  (draw-parts)
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
                              "ArrowUp" (.send ws {:direction :up :cake true})
                              "ArrowDown" (.send ws {:direction :down :cake true})
                              "ArrowLeft" (.send ws {:direction :left :cake true})
                              "ArrowRight" (.send ws {:direction :right :cake true}))))]
    (.addEventListener js/document "keydown" handle-keypress)
    (.addEventListener ws "open" (fn [_]
                                   (let [id (get-player-id)]
                                     (reset! player-id id)
                                     (.send ws {:id id :game-mode (:cake game-mode-enum)}))))
    (.addEventListener ws "message" (fn [e]
                                      (let [data (edn/read-string (.-data e))] 
                                        (if (:winner data)
                                          (do (reset! stop-game-flag true)
                                              (js/setTimeout #(stop-game data) 250))
                                          (reset! game-state data)))

                                      (when (not @loading-flag)
                                        (disable-loading)
                                        (reset! end-score-data nil)
                                        (reset! loading-flag true))))
    (.addEventListener ws "close" (fn [_] (println "WebSocket closed")
                                    (.removeEventListener js/document "keydown" handle-keypress)))))



