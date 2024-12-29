(ns server.core
  (:gen-class)
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [defroutes GET]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.websocket :as ws]
   [server.game :refer [change-direction start-game stop-game]]))

(def online-players (atom [{:id 6, :socket "smth", :in-game true}]))

;try to find available players and start the game
(defn find-game [player-socket player-id]
  (let [new-player {:id player-id :socket player-socket :in-game false}]
    (if (empty? @online-players)
      (swap! online-players conj new-player)
      (let [player (some #(when (= false (:in-game %)) %) @online-players)]
        (if player
          (do
            (swap! online-players (fn [players] (filterv #(not= (:id %) (:id player)) players)))
            (start-game player new-player))
          (swap! online-players conj new-player))))))


;process messages form client's web socket
(defn handle-message [socket message]
  (let [data (edn/read-string message)
        dir (:direction data)]
    (if (int? (:id data))
      (find-game socket (:id data))
      (change-direction socket dir))))

(defn echo-handler [request]
  (assert (ws/upgrade-request? request))
  {::ws/listener
   {:on-open
    (fn [socket])
    :on-message
    (fn [socket message]
      (if (= message "exit")
        (ws/close socket)
        (handle-message socket message)))
    :on-close
    (fn [socket code reason] stop-game)}})

(defroutes app-routes
  (GET "/" [] "<h1>HELLO<h1>")
  (GET "/ws" [] echo-handler))

(defn -main []
  (run-jetty app-routes {:port 8080 :join? false}))
