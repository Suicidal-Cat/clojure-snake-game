(ns client.game
   (:require
    [reagent.core :as r]
    [quil.core :as q]
    [clojure.edn :as edn]))

; Atom za čuvanje Base64 stringa 
(def img-atom (atom nil)) 


(def snake-body1 (r/atom [[0 0]]))
(def snake-body2 (r/atom [[0 0]]))
(def ball (r/atom [0 0]))
(def score (r/atom [0 0]))
(def field-size 600) ;field size in px
(def grid-size 24) ;grid size in px


 (defn connect_socket []
   (let [ws (js/WebSocket. "ws://localhost:8080/ws")]
     (.addEventListener ws "open" (fn [_]
                                    (println "WebSocket connected")
                                    (.send ws {:id (rand-int 1000)})))
     (.addEventListener ws "message" (fn [e]
                                       (let [data (edn/read-string (.-data e))]
                                        ;(println data)
                                         (reset! ball (:ball data))
                                         (reset! snake-body1 (:snake1 data))
                                         (reset! snake-body2 (:snake2 data))
                                         (reset! score (:score data)))))
     (.addEventListener ws "close" (fn [_] (println "WebSocket closed")))
     (let [handle-keypress (fn handle-keypress [e]
                             (let [key (.-key e)]
                               (case key
                                 "ArrowUp" (.send ws {:direction :up})
                                 "ArrowDown" (.send ws {:direction :down})
                                 "ArrowLeft" (.send ws {:direction :left})
                                 "ArrowRight" (.send ws {:direction :right}))))]
       (.addEventListener js/document "keydown" handle-keypress))))

 (defn setup []
   (let [
         url "/images/snake2.png"]
     (q/set-state! :image (q/load-image url)))
   (q/frame-rate 30)
   (q/background 0))

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
 
(defn draw []
  (q/background 0)
  (draw-grid-border grid-size)
  (q/fill 0 0 255)
  (q/ellipse (first @ball) (last @ball) 20 20)
  (q/stroke 0)
  (q/stroke-weight 0)
  (q/fill 0 255 0)
  (doseq [[x y] @snake-body1]
    (q/rect x y grid-size grid-size))
  (q/fill 255 0 0)
  ;; (doseq [[x y] @snake-body2]
  ;;   (q/rect x y grid-size grid-size))
  (let [im (q/state :image)]
    (doseq [[x y] @snake-body2]
      (q/image im x y))))


(defn start_game []
  (q/sketch
   :host "game-canvas"
   :settings #(q/smooth 2)
   :setup setup
   :draw draw
   :size [field-size field-size]))

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



