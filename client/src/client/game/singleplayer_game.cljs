(ns client.game.singleplayer-game
  (:require
   [client.helper-func :as hf :refer [get-player-id save-region-screenshot!]]
   [clojure.edn :as edn]
   [quil.core :as q]
   [reagent.core :as r]))


(def score (r/atom [0 0]))
(def game-state (r/atom nil))
(def field-size 600) ;field size in px
(def grid-size 30) ;grid size in px
(def stop-game-flag (atom false))
(def player-id (atom 0))


;stoping game
(defn stop-game [data]
  (let [winner (:winner data)
        [hx hy] (mapv #(* % 2) (:head winner))]
    (save-region-screenshot! (max 0 (- hx 180)) (max 0 (- hy 180)) 400 400)
    (reset! stop-game-flag true)))

;websocket communication
(defn connect_socket []
  (let [ws (js/WebSocket. "ws://localhost:8085/ws")
        handle-keypress (fn handle-keypress [e]
                          (let [key (.-key e)]
                            (case key
                              "ArrowUp" (.send ws {:direction :up :single true})
                              "ArrowDown" (.send ws {:direction :down :single true})
                              "ArrowLeft" (.send ws {:direction :left :single true})
                              "ArrowRight" (.send ws {:direction :right :single true}))))]
    (.addEventListener js/document "keydown" handle-keypress)
    (.addEventListener ws "open" (fn [_]
                                   (reset! stop-game-flag false)
                                   (let [id (get-player-id)]
                                     (reset! player-id id)
                                     (.send ws {:id id :single true}))))
    (.addEventListener ws "message" (fn [e]
                                      (let [data (edn/read-string (.-data e))]
                                        (reset! game-state data)
                                        (reset! score (:score data))
                                        (when (:winner data) (stop-game data)))))
    (.addEventListener ws "close" (fn [_] (println "WebSocket closed")
                                    (.removeEventListener js/document "keydown" handle-keypress)))))

;canvas setup
(defn setup []
  (let [url "/images/snake22.png"]
    (q/set-state! :image (q/load-image url)))
  (q/frame-rate 30)
  (q/background 0))

(defn draw-grid-border [grid-size]
  (q/fill 148 148 148)
  (q/stroke 50)
  (q/stroke-weight 1)
  (q/fill 0 0 0)
  (doseq [x (range 0 (q/width) grid-size)]
    (q/line x 0 x (q/height)))
  (doseq [y (range 0 (q/height) grid-size)]
    (q/line 0 y (q/width) y)))

;draw food
(defn draw-food []
  (q/fill 0 0 255)
  (q/ellipse (first (:ball @game-state)) (last (:ball @game-state)) 27 27))

;draw snake
(defn draw-snake []
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill 255 0 0)
  (let [im (q/state :image)]
    (doseq [[x y] (:snake1 @game-state)]
      (q/image im x y))))

;stop drawing
(defn stop-drwing []
  (when @stop-game-flag (q/no-loop)))

;main draw
(defn draw []
  (q/background 0)
  (draw-grid-border grid-size)
  (draw-food)
  (draw-snake)
  (stop-drwing))

;start the game
(defn start_game []
  (q/sketch
   :host "game-canvas"
   :settings #(q/smooth 2)
   :setup setup
   :draw draw
   :size [field-size field-size]))