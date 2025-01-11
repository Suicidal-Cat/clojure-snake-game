(ns client.main-game
   (:require
    [reagent.core :as r]
    [quil.core :as q]
    [clojure.edn :as edn]))

; Atom za čuvanje ss-a
(def img-atom (atom nil)) 

(def score (r/atom [0 0]))
(def game-state (r/atom nil))
(def field-size 594) ;field size in px
(def grid-size 27) ;grid size in px
(def stop-game-flag (atom false))
(def random-id (atom 0))


;take a screenshoot of the canvas
(defn save-region-screenshot! [x y width height]
  (let [canvas (.getElementById js/document "defaultCanvas0")
        ctx (.getContext canvas "2d")
        image-data (.getImageData ctx x y width height)
        temp-canvas (js/document.createElement "canvas")
        temp-ctx (.getContext temp-canvas "2d")]
    (set! (.-width temp-canvas) width)
    (set! (.-height temp-canvas) height)
    (.putImageData temp-ctx image-data 0 0)
    (let [base64 (.toDataURL temp-canvas)]
      (reset! img-atom base64))))

;stoping game
(defn stop-game [data]
  (let [winner (:winner data)
        [hx hy] (mapv #(* % 2) (:head winner))
        loser (:loser data)] 
    (save-region-screenshot! (max 0 (- hx 180)) (max 0 (- hy 180)) 400 400)
    (reset! stop-game-flag true)))

;websocket communication
(defn connect_socket []
  (let [ws (js/WebSocket. "ws://localhost:8080/ws")]
    (.addEventListener ws "open" (fn [_]
                                   (reset! stop-game-flag false)
                                   (let [id (rand-int 1000)]
                                     (reset! random-id id)
                                     (.send ws {:id id}))))
    (.addEventListener ws "message" (fn [e]
                                      (let [data (edn/read-string (.-data e))]
                                        (reset! game-state data)
                                        (reset! score (:score data)) 
                                        (when (:winner data) (stop-game data)))))
    (.addEventListener ws "close" (fn [_] (println "WebSocket closed")))
    (let [handle-keypress (fn handle-keypress [e]
                            (let [key (.-key e)]
                              (case key
                                "ArrowUp" (.send ws {:direction :up})
                                "ArrowDown" (.send ws {:direction :down})
                                "ArrowLeft" (.send ws {:direction :left})
                                "ArrowRight" (.send ws {:direction :right}))))]
      (.addEventListener js/document "keydown" handle-keypress))))

;canvas setup
(defn setup []
  (let [url "/images/snake27.png"]
    (q/set-state! :image (q/load-image url)))
  (q/frame-rate 30)
  (q/background 0))

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

;main draw
(defn draw []
  (q/background 0)
  (draw-grid-border grid-size)
  (q/fill 0 0 255)
  (q/ellipse (first (:ball @game-state)) (last (:ball @game-state)) 20 20)
  (when-let [power (:power @game-state)]
    (case (:value power)
      "+3" (do (q/fill 255 0 0) (q/ellipse (first (:cord power)) (last (:cord power)) 20 20))
      "-3" (do (q/fill 0 255 0) (q/ellipse (first (:cord power)) (last (:cord power)) 20 20))))
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill 0 255 0)
  (doseq [[x y] (:snake1 @game-state)]
    (q/rect x y grid-size grid-size))
  (q/fill 255 0 0)
  (let [im (q/state :image)]
    (doseq [[x y] (:snake2 @game-state)]
      (q/image im x y)))
  (when @stop-game-flag (q/no-loop)))

;start the game
(defn start_game []
  (q/sketch
   :host "game-canvas"
   :settings #(q/smooth 2)
   :setup setup
   :draw draw
   :size [field-size field-size]))



