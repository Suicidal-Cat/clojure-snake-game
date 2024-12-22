(ns client.core
    (:require [reagent.dom :as rdom]
              [reagent.core :as r]
              [quil.core :as q]
              [clojure.edn :as edn]
              ))

(enable-console-print!)


(defonce app-state (atom {:text "Hello world!"}))
(def snake-body1 (r/atom [[180 100] [160 100] [140 100] [120 100] [100 100]]))
(def snake-body2 (r/atom [[300 120] [300 140] [300 160] [300 180] [300 200]]))


(defn on-js-reload []
)

(def ws (js/WebSocket. "ws://localhost:8080/ws"))

(.addEventListener ws "open" (fn [_] (println "WebSocket connected")))
(.addEventListener ws "message" (fn [e]
                                  (let [data (edn/read-string (.-data e))]
                                    ;(println data)
                                    (reset! snake-body1 (:snake1 data))
                                    (reset! snake-body2 (:snake2 data))
                                    )))
(.addEventListener ws "close" (fn [_] (println "WebSocket closed")))

(defn handle-keypress [e]
  (let [key (.-key e)]
    (case key
      "ArrowUp" (.send ws {:direction :up})
      "ArrowDown" (.send ws {:direction :down})
      "ArrowLeft" (.send ws {:direction :left})
      "ArrowRight" (.send ws {:direction :right}))))

(.addEventListener js/document "keydown" handle-keypress)

(defn canvas []
  [:div {:id "game-canvas"}])

(defn app []
  [:div {:class "game"}
   [:p "Snake game"]
   [canvas]])

(defn setup []
  (q/frame-rate 25) 
  (q/background 0))

(defn draw []
  (q/background 0)
  (q/stroke 0)
  (q/stroke-weight 2)
  (q/fill 0 255 0)
  (doseq [[x y] @snake-body1]
    (q/rect x y 20 20))
  (q/fill 255 0 0)
  (doseq [[x y] @snake-body2]
    (q/rect x y 20 20)))

(rdom/render [app] (.getElementById js/document "app"))

(q/sketch
 :title "Snake game"
 :host "game-canvas"
 :settings #(q/smooth 2)
 :setup setup
 :draw draw
 :size [700 500])

