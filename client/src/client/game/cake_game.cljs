(ns client.game.cake-game
  (:require
   [client.game.game-helper-func :refer [draw-grid-standard
                                         draw-player-indicator draw-snake
                                         random-snake-images]]
   [client.helper-func :as hf :refer [game-mode-enum get-player-id
                                      save-region-screenshot! show-end-dialog]]
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
        [hx hy] (mapv #(* % 2) (:head winner))]
    (save-region-screenshot! (max 0 (- hx 180)) (max 0 (- hy 180)) 400 400)
    (reset! end-score-data data)
    (reset! stop-game-flag true)
    (reset! show-end-dialog true)))

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
  (swap! (q/state-atom) assoc :indicator (q/load-image "/images/indicator.png") :ind-y 0)
  (q/frame-rate 30)
  (q/background 0))

;draw food
(defn draw-parts []
  (doseq [part (:parts @game-state)]
    (let [[x y] (:coordinate part)]
      (q/image (q/state (keyword (:image part))) x y grid-size grid-size))))

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
  (draw-parts)
  (draw-snakes)
  (draw-player-indicator "indicator" game-state player-id)
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
                              "ArrowUp" (.send ws {:direction :up :cake true})
                              "ArrowDown" (.send ws {:direction :down :cake true})
                              "ArrowLeft" (.send ws {:direction :left :cake true})
                              "ArrowRight" (.send ws {:direction :right :cake true}))))]
    (.addEventListener js/document "keydown" handle-keypress)
    (.addEventListener ws "open" (fn [_]
                                   (reset! stop-game-flag false)
                                   (let [id (get-player-id)]
                                     (reset! player-id id)
                                     (.send ws {:id id :game-mode (:cake game-mode-enum)}))))
    (.addEventListener ws "message" (fn [e]
                                      (let [data (edn/read-string (.-data e))]
                                        (reset! game-state data)
                                        (when (:winner data) (stop-game data)))

                                      (when (not @loading-flag)
                                        (disable-loading)
                                        (reset! end-score-data nil)
                                        (reset! loading-flag true))))
    (.addEventListener ws "close" (fn [_] (println "WebSocket closed")
                                    (.removeEventListener js/document "keydown" handle-keypress)))))



