(ns client.game
  (:require 
            [reagent.core :as r]
            [quil.core :as q]
            [clojure.edn :as edn]))

(def snake-body1 (r/atom [[100 100]]))
(def snake-body2 (r/atom [[300 200]]))
(def ball (r/atom [290 290]))


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
                                        (reset! snake-body2 (:snake2 data)))))
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
  (q/frame-rate 25)
  (q/background 0))

(defn draw-grid-border [grid-size]
  (q/fill 148 148 148)
  (q/rect 0 0 (q/width) grid-size)
  (q/rect 0 0 grid-size (q/height))
  (q/rect 0 (- (q/height) grid-size) (q/width) grid-size)
  (q/rect (- (q/width) grid-size) 0 grid-size (q/height))
  (q/stroke 50)
  (q/fill 0 0 0)
  (doseq [x (range grid-size (- (q/width) 20) grid-size)]
    (q/line x 0 x (q/height)))
  (doseq [y (range grid-size (- (q/height) 20) grid-size)]
    (q/line 0 y (q/width) y)))

(defn draw []
  (q/background 0)
  (draw-grid-border 20)
  (q/stroke 0)
  (q/stroke-weight 2) 
  (q/fill 0 0 255)
  (q/ellipse (first @ball) (last @ball) 18 18)
  (q/fill 0 255 0)
  (doseq [[x y] @snake-body1]
    (q/rect x y 20 20))
  (q/fill 255 0 0)
  (doseq [[x y] @snake-body2]
    (q/rect x y 20 20)))


(defn start_game []
  (q/sketch
   :title "Snake game"
   :host "game-canvas"
   :settings #(q/smooth 2)
   :setup setup
   :draw draw
   :size [600 600])
  )



