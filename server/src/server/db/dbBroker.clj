(ns server.db.dbBroker
  (:require
   [aero.core :refer [read-config]]
   [clojure.edn :as edn]
   [next.jdbc :as jdbc]))

(def config (read-config "config.edn"))
(def ds (if (:disableDB config) nil (jdbc/get-datasource (:db-config config))))

(defn get-gametypes []
  (when ds (jdbc/execute! ds ["SELECT * FROM Gametypes"])))

(defn login [username password]
  (when ds 
    (let [data (jdbc/execute! ds ["SELECT * FROM Users WHERE Username=? and Password=? and IsActive=1" username password])
          user (if data (first data) nil)]
      (if user {:id (:users/Id user) :username (:users/Username user)} nil))))