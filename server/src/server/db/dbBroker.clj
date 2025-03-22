(ns server.db.dbBroker
  (:require
   [aero.core :refer [read-config]]
   [clojure.edn :as edn]
   [next.jdbc :as jdbc]
   [buddy.hashers :as hashers])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

(defn current-datetime []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format (LocalDateTime/now) formatter)))

(def config (read-config "config.edn"))
(def ds (if (:disableDB config) nil (jdbc/get-datasource (:db-config config))))

(defn get-gametypes []
  (when ds (jdbc/execute! ds ["SELECT * FROM Gametypes"])))

(defn login [email password]
  (when ds 
    (let [data (jdbc/execute! ds ["SELECT * FROM Users WHERE email=? and IsActive=1" email])
          user (if data (first data) nil)]
      (if (and user (hashers/check password (:users/Password user)))
        (do (jdbc/execute! ds ["UPDATE Users SET LastLogin=? WHERE id=?" (current-datetime) (:users/Id user)])
            {:id (:users/Id user) :username (:users/Username user)})
        nil))))

(defn register [email username password]
  (when ds
    (let [hashed-password (hashers/derive password)
          data (jdbc/execute! ds ["INSERT INTO Users (Email, Username, Password, IsActive, CreatedAt) VALUES (?,?,?,1,?)" email username hashed-password (current-datetime)])]
      (if data true nil))))