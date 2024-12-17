(ns client.core
    (:require [reagent.dom :as rdom]
              [reagent.core :as r]
              [quil.core :as q]
              ))

(enable-console-print!)


(defonce app-state (atom {:text "Hello world!"}))


(defn on-js-reload []
)


;; (def socket
;;   (ws/connect
;;    "ws://localhost:8080/ws"
;;    :on-receive #(prn 'received %)))
;; ;(ws/send-msg socket "meow32121")
;; ;(ws/close socket)

(def ws (js/WebSocket. "ws://localhost:8080/ws"))

(.addEventListener ws "open" (fn [_] (println "WebSocket connected")  (.send ws "Hello from ClojureScript!")))
(.addEventListener ws "message" (fn [e] (println "Message received:" (.-data e))))
(.addEventListener ws "close" (fn [_] (println "WebSocket closed")))


(defn canvas []
  [:div {:id "game-canvas"}])

(defn app []
  [:div {:class "game"}
   [:p "Snake game"]
   [canvas]])

(def snake-body1 (r/atom [[100 100] [120 100] [140 100] [160 100] [180 100]]))
(def snake-body2 (r/atom [[300 120] [300 140] [300 160] [300 180] [300 200]]))

(defn setup []
  (q/frame-rate 1) 
  (q/background 0))

(defn draw []
  (q/background 0)
  (q/stroke 0)     ;; Crna ivica
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
 :size [800 600])

