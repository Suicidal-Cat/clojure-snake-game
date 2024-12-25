(ns server.core
  (:gen-class)
  (:require [compojure.core :refer [defroutes GET]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.websocket :as ws]
            [clojure.edn :as edn]))

;hash-map - :snake array [X Y]
(def game-state (atom {:snake1 [[100 100] [120 100] [140 100] [160 100] [180 100]]
                       :snake2 [[300 120] [300 140] [300 160] [300 180] [300 200]]}))

;hash-map - :playerid :direction
(def snakes-direction (atom {:snake1 :right
                            :snake2 :left}))
;update snake by direction
(defn move-snake [snake direction]
  (let [[x y] (first snake)]
    (case direction
      :up    (cons [x (- y 20)] (butlast snake))
      :down  (cons [x (+ y 20)] (butlast snake))
      :left  (cons [(- x 20) y] (butlast snake))
      :right (cons [(+ x 20) y] (butlast snake)))))

;update direction
(defn change-direction [player dir]
  (let [past-dir (player @snakes-direction)]
    (if (not= past-dir dir)
      (cond
        (and (= past-dir :up) (not= dir :down)) (swap! snakes-direction update player (fn [_] dir))
        (and (= past-dir :down) (not= dir :up)) (swap! snakes-direction update player (fn [_] dir))
        (and (= past-dir :left) (not= dir :right)) (swap! snakes-direction update player (fn [_] dir))
        (and (= past-dir :right) (not= dir :left)) (swap! snakes-direction update player (fn [_] dir))))))

(defn handle-message [message]
  (let [data (edn/read-string message)
        dir (:direction data)]
    (if (int? (:id data))
      (println (:id data))
      (change-direction :snake1 dir))))

(def stop-flag (atom false))

(defn broadcast-game-state [socket]
  (future
    (reset! stop-flag false)
    (while (not @stop-flag)
      (Thread/sleep 90)
      (swap! game-state update :snake1 move-snake (:snake1 @snakes-direction))
      (ws/send socket (pr-str @game-state)))))


(defn echo-handler [request]
  (assert (ws/upgrade-request? request))
  {::ws/listener
   {:on-open
    (fn [socket]
      (reset! game-state {:snake1 [[180 100] [160 100] [140 100] [120 100] [100 100]]
                          :snake2 [[300 120] [300 140] [300 160] [300 180] [300 200]]})
      (broadcast-game-state socket))
    :on-message
    (fn [socket message]
      (if (= message "exit")
        (ws/close socket)
        (handle-message message)))
    :on-close
    (fn [socket code reason]
      (reset! stop-flag true))}})

(defroutes app-routes
  (GET "/" [] "<h1>HELLO<h1>")
  (GET "/ws" [] echo-handler))

(defn -main []
  (run-jetty app-routes {:port 8080 :join? false}))
