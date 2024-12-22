(ns server.core
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.websocket :as ws]
            [clojure.edn :as edn]
            ))


(def game-state (atom {:snake1 [[100 100] [120 100] [140 100] [160 100] [180 100]]
                       :snake2 [[300 120] [300 140] [300 160] [300 180] [300 200]]}))

(def snake-direction (atom {:snake1 :right
                      :snake2 :left}))

(defn move-snake [snake direction]
  (let [[x y] (first snake)]
    (case direction
      :up    (cons [x (- y 20)] (butlast snake))
      :down  (cons [x (+ y 20)] (butlast snake))
      :left  (cons [(- x 20) y] (butlast snake))
      :right (cons [(+ x 20) y] (butlast snake)))))

(defn change-direction [snake dir]
  (let [past-dir (snake @snake-direction)]
    (if (not= past-dir dir)
      (cond
        (and (= past-dir :up) (not= dir :down)) (swap! snake-direction update snake (fn [_] dir))
        (and (= past-dir :down) (not= dir :up)) (swap! snake-direction update snake (fn [_] dir))
        (and (= past-dir :left) (not= dir :right)) (swap! snake-direction update snake (fn [_] dir))
        (and (= past-dir :right) (not= dir :left)) (swap! snake-direction update snake (fn [_] dir))))))

(defn handle-message [message]
  (let [data (edn/read-string message)
        dir (:direction data)]
    (println dir)
    (change-direction :snake1 dir)
    ))

(def stop-flag (atom false))

(defn broadcast-game-state [socket]
  (future
    (reset! stop-flag false)
    (while (not @stop-flag)
      (Thread/sleep 100)
      (swap! game-state update :snake1 move-snake (:snake1 @snake-direction))
      (ws/send socket (pr-str @game-state)))))


(defn echo-handler [request]
  (assert (ws/upgrade-request? request))
  {::ws/listener
   {:on-open
    (fn [socket]
      ;(ws/send socket "I will echo your messages")
      (reset! game-state {:snake1 [[180 100] [160 100] [140 100] [120 100] [100 100]]
                          :snake2 [[300 120] [300 140] [300 160] [300 180] [300 200]]})
      (broadcast-game-state socket)
      )
    :on-message
    (fn [socket message]
      (if (= message "exit")
        (ws/close socket) 
        (handle-message message)
        ))
    :on-close
    (fn [socket code reason]
      (reset! stop-flag true))
    }})

(defroutes app-routes
  (GET "/" [] "<h1>HELLO<h1>")
  (GET "/ws" [] echo-handler))

(defn -main []
  (run-jetty app-routes {:port 8080 :join? false}))
