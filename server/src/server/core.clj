(ns server.core
  (:gen-class)
  (:require
   [aero.core :refer [read-config]]
   [clojure.edn :as edn]
   [compojure.core :refer [defroutes GET]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.websocket :as ws]
   [server.main-game :refer [change-direction start-game]]
   [server.singleplayer-game :refer [change-direction-single start-game-single]]
   [server.db.dbBroker :as db]))

(def config (read-config "config.edn"))
(def online-players (atom []))

;try to find available player and start the game
(defn find-game [player-socket data]
  (let [new-player {:id (:id data) :socket player-socket :in-game false}]
    (if (:single data) (start-game-single new-player)
        (if (empty? @online-players)
          (swap! online-players conj new-player)
          (let [player (some #(when (= false (:in-game %)) %) @online-players)]
            (if player
              (do
                (swap! online-players (fn [players] (filterv #(not= (:id %) (:id player)) players)))
                (start-game player new-player))
              (swap! online-players conj new-player)))))))


;process messages form client's web socket
(defn handle-message [socket message]
  (let [data (edn/read-string message)
        dir (:direction data)]
    (if (int? (:id data))
      (find-game socket data)
      (if (:single data)
        (change-direction-single socket dir)
        (change-direction socket dir)))))

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
    (fn [socket code reason] ())}})

(defroutes app-routes
  (GET "/" [] "<h1>HELLO<h1>")
  (GET "/ws" [] echo-handler))

(defn -main []
  (run-jetty app-routes {:port (:server-port config) :join? false}))
