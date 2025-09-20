(ns client.game.singleplayer-game
  (:require
   [client.game.game-helper-func :refer [draw-grid-standard draw-snake
                                         get-food-image pulse-normal
                                         random-snake-image]]
   [client.helper-func :as hf :refer [api-domain get-player-id
                                      save-region-screenshot! show-end-dialog]]
   [clojure.edn :as edn]
   [quil.core :as q]
   [reagent.core :as r]))


(def score (r/atom [0]))
(def end-score-data (r/atom nil))
(def game-state (r/atom nil))
(def field-size 594) ;field size in px
(def grid-size 33) ;grid size in px
(def stop-game-flag (atom false))
(def loading-flag (atom false))
(def player-id (atom 0))

;stoping game
(defn stop-game [data]
  (let [winner (:winner data)
        [hx hy] (:head winner)]
    (save-region-screenshot! (max 0 (- hx 100)) (max 0 (- hy 100)) (* 2 (min 340 (- field-size (- hx 340)))) (* 2 (min 340 (- field-size (- hx 340)))))
    (reset! end-score-data data)))

;canvas setup
(defn setup []
  (let [head "/images/hgreen.png"
        body "/images/bgreen.png"
        food-image (get-food-image)]
    (q/set-state! :head (q/load-image head) 
                  :body (q/load-image body)
                  :food-img (q/load-image (str "/images/parts/" food-image))
                  :radius grid-size))
  (doseq [[k v] (random-snake-image)]
    (swap! (q/state-atom) assoc k v))
  (q/frame-rate 30)
  (q/background 0))

;draw food
(defn draw-food []
  (when-let [[x y] (:ball @game-state)]
    (let [r (q/state :radius)]
      (q/image-mode :center)
      (q/image (q/state :food-img) (+ x (/ grid-size 2)) (+ y (/ grid-size 2)) r r)
      (q/image-mode :corner))))

;stop drawing
(defn stop-drawing []
  (when @stop-game-flag (q/no-loop)))

;main draw
(defn draw []
  (q/background 0) 
  (swap! (q/state-atom) assoc :radius (pulse-normal 32 37))
  (draw-grid-standard grid-size)
  (draw-food)
  (draw-snake (q/state :head) (q/state :body) (:snake1 @game-state) grid-size)
  (stop-drawing))

;start the game
(defn start-game []
  (reset! stop-game-flag false)
  (reset! game-state nil)
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
                              "ArrowUp" (.send ws {:direction :up :single true})
                              "ArrowDown" (.send ws {:direction :down :single true})
                              "ArrowLeft" (.send ws {:direction :left :single true})
                              "ArrowRight" (.send ws {:direction :right :single true}))))]
    (.addEventListener js/document "keydown" handle-keypress)
    (.addEventListener ws "open" (fn [_]
                                   (let [id (get-player-id)]
                                     (reset! player-id id)
                                     (.send ws {:id id :single true}))))
    (.addEventListener ws "message" (fn [e]
                                      (when-let [data (edn/read-string (.-data e))]
                                        (if (:winner data)
                                          (do
                                            (reset! stop-game-flag true)
                                            (js/setTimeout #(stop-game data) 250))
                                          (do (reset! game-state data)
                                              (reset! score (:score data)))))

                                      (when (not @loading-flag)
                                        (disable-loading)
                                        (reset! end-score-data nil)
                                        (reset! loading-flag true))))
    (.addEventListener ws "close" (fn [_] (println "WebSocket closed")
                                    (.removeEventListener js/document "keydown" handle-keypress)))))