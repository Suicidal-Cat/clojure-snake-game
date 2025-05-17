(ns server.db.dbBroker
  (:require
   [aero.core :refer [read-config]]
   [clojure.edn :as edn]
   [next.jdbc :as jdbc]
   [buddy.hashers :as hashers])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

;; game types enum
(def game-types-enum {:singleplayer "Singleplayer" :multiplayer "Multiplayer"})

;; returns current datetime
(defn current-datetime []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format (LocalDateTime/now) formatter)))

;; get config
(def config (read-config "config.edn"))
(def ds (if (:disableDB config) nil (jdbc/get-datasource (:db-config config))))

;; get game-types from database
(defn get-gametypes []
  (when ds (jdbc/execute! ds ["SELECT * FROM Gametypes"])))

;; login user
(defn login [email password]
  (when ds
    (let [data (jdbc/execute! ds ["SELECT * FROM Users WHERE email=? and IsActive=1" email])
          user (if data (first data) nil)]
      (if (and user (hashers/check password (:users/Password user)))
        (do (jdbc/execute! ds ["UPDATE Users SET LastLogin=? WHERE id=?" (current-datetime) (:users/Id user)])
            {:id (:users/Id user) :username (:users/Username user)})
        nil))))

;; register user
(defn register [email username password]
  (when ds
    (let [hashed-password (hashers/derive password)
          data (jdbc/execute! ds ["INSERT INTO Users (Email, Username, Password, IsActive, CreatedAt) VALUES (?,?,?,1,?)" email username hashed-password (current-datetime)])]
      (if data true nil))))

;; saves game result
(defn save-game [game-result isMultiPlayer]
  (when ds
    (let [winnerId (:id (:winner game-result))
          loserId (:id (:loser game-result))
          score (str (:score (:winner game-result)) " - " (if isMultiPlayer (:score (:loser game-result)) 0))
          game-types (get-gametypes)
          game-type (if isMultiPlayer (:multiplayer game-types-enum) (:singleplayer game-types-enum))
          game-typeId (some #(when (= (:gametypes/Name %) game-type) (:gametypes/Id %)) game-types)]
      (jdbc/execute! ds
                     ["INSERT INTO Games (UserId1, UserId2, Score, WinnerId, GameTypeId, CreatedAt) 
                                     VALUES (?,?,?,?,?,?)" winnerId loserId score winnerId game-typeId (current-datetime)]))))