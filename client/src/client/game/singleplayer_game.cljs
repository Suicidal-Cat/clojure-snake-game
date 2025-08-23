(ns client.game.singleplayer-game
  (:require
   [client.game.game-helper-func :refer [draw-grid-main draw-snake
                                         get-food-image random-snake-image]]
   [client.helper-func :as hf :refer [get-player-id save-region-screenshot!]]
   [clojure.edn :as edn]
   [quil.core :as q]
   [reagent.core :as r]))


(def score (r/atom [0 0]))
(def game-state (r/atom nil))
(def field-size 594) ;field size in px
(def grid-size 33) ;grid size in px
(def stop-game-flag (atom false))
(def player-id (atom 0))

;stoping game
(defn stop-game [data]
  (let [winner (:winner data)
        [hx hy] (mapv #(* % 2) (:head winner))]
    (save-region-screenshot! (max 0 (- hx 180)) (max 0 (- hy 180)) 400 400)
    (reset! stop-game-flag true)))

;canvas setup
(defn setup []
  (let [head "/images/hgreen.png"
        body "/images/bgreen.png"
        food-image (get-food-image)]
    (q/set-state! :head (q/load-image head) 
                  :body (q/load-image body)
                  :food-img (q/load-image (str "/images/parts/" food-image))))
  (doseq [[k v] (random-snake-image)]
    (swap! (q/state-atom) assoc k v))
  (q/frame-rate 30)
  (q/background 0))

;draw food
(defn draw-food []
  (when (:ball @game-state)
    (q/image (q/state :food-img) ((:ball @game-state) 0) ((:ball @game-state) 1) grid-size grid-size)))

;stop drawing
(defn stop-drawing []
  (when @stop-game-flag (q/no-loop)))

;main draw
(defn draw []
  (q/background 0)
  (draw-grid-main grid-size)
  (draw-food)
  (draw-snake (q/state :head) (q/state :body) (:snake1 @game-state) grid-size)
  (stop-drawing))

;start the game
(defn start_game []
  (q/sketch
   :host "game-canvas"
   :settings #(q/smooth 2)
   :setup setup
   :draw draw
   :size [field-size field-size]))

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