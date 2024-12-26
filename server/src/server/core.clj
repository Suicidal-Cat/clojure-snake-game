(ns server.core
  (:gen-class)
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [defroutes GET]]
   [ring.adapter.jetty :refer [run-jetty]]
   [ring.websocket :as ws]
   [server.game :refer [broadcast-game-state change-direction stop-game]]))

(defn handle-message [message]
  (let [data (edn/read-string message)
        dir (:direction data)]
    (if (int? (:id data))
      (println (:id data))
      (change-direction :snake1 dir))))


(defn echo-handler [request]
  (assert (ws/upgrade-request? request))
  {::ws/listener
   {:on-open
    (fn [socket] 
      (broadcast-game-state socket))
    :on-message
    (fn [socket message]
      (if (= message "exit")
        (ws/close socket)
        (handle-message message)))
    :on-close
    (fn [socket code reason]
      stop-game)}})

(defroutes app-routes
  (GET "/" [] "<h1>HELLO<h1>")
  (GET "/ws" [] echo-handler))

(defn -main []
  (run-jetty app-routes {:port 8080 :join? false}))
