(ns server.core
  (:gen-class)
  (:require [compojure.core :refer :all]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.websocket :as ws]
            ;[ring.util.response :refer [file-response]]
            ))

(defn echo-handler [request]
  (assert (ws/upgrade-request? request))
  {::ws/listener
   {:on-open
    (fn [socket]
      (ws/send socket "I will echo your messages"))
    :on-message
    (fn [socket message]
      (if (= message "exit")
        (ws/close socket)
        (ws/send socket message)))}})

(defroutes app-routes
  (GET "/" [] "<h1>HELLO<h1>")
  (GET "/ws" [] echo-handler))

(defn -main []
  (run-jetty app-routes {:port 8080 :join? false}))
