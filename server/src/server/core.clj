(ns server.core
  (:gen-class)
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [defroutes GET]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.websocket :as ws]
   [server.game :refer [change-direction start-game stop-game]]))

(def online-players (atom [{:id 6, :socket "smth", :in-game true}]))

;trying to find available players and start the game
(defn find-game [player-socket player-id]
  (if (empty? @online-players)
    (swap! online-players conj {:id player-id :socket player-socket :in-game false})
    (let [player (some #(when (= false (:in-game %)) %) @online-players)]
      (if player
        (do
          (swap! online-players (fn [players] (filterv #(not= (:id %) (:id player)) players)))
          (start-game player {:id player-id :socket player-socket :in-game false}))
        (swap! online-players conj {:id player-id :socket player-socket :in-game false})))))


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
