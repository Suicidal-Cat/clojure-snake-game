(ns client.core
    (:require [gniazdo.core :as ws]))

(enable-console-print!)


(defonce app-state (atom {:text "Hello world!"}))




(defn on-js-reload []
)


(def socket
  (ws/connect
   "ws://localhost:8080/ws"
   :on-receive #(prn 'received %)))
;(ws/send-msg socket "meow32121")
;(ws/close socket)

