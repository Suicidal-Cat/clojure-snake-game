(ns server.db.dbBroker
  (:require
   [aero.core :refer [read-config]]
   [clojure.edn :as edn]
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]))

(def config (read-config "config.edn"))
(def ds (if (:disableDB config) nil (jdbc/get-datasource (:db-config config))))

(defn get-gametypes []
  (when ds (jdbc/execute! ds ["SELECT * FROM Gametypes"])))