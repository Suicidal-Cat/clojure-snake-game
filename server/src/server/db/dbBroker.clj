(ns server.db.dbBroker
  (:require
   [aero.core :refer [read-config]]
   [next.jdbc :as jdbc]
   [next.jdbc.sql :as sql]
   [buddy.hashers :as hashers]
   [clojure.string :as str])
  (:import [java.time LocalDateTime]
           [java.time.format DateTimeFormatter]))

;; game types enum
(def game-types-enum {:singleplayer "Singleplayer" :multiplayer "Multiplayer"})
;; friend request statuses
(def friendship-status {:pending "Pending" :accepted "Accepted" :declined "Declined"})

;; returns current datetime
(defn current-datetime []
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")]
    (.format (LocalDateTime/now) formatter)))

(defn normalize-db-result [rows]
  (map (fn [row]
         (->> row
              (map (fn [[k v]]
                     [(-> k name str/lower-case keyword)
                      (if (decimal? v) (double v) v)]))
              (into {})))
       rows))
;; get config
(def config (read-config "config.edn"))
(def ds (if (:disableDB config) nil (jdbc/get-datasource (:db-config config))))

;; get game-types from database
(defn get-gametypes []
  (when ds (jdbc/execute! ds ["SELECT * FROM Gametypes"])))

;; get user by id
(defn get-user-by-id [user-id]
  (when ds (let [user (jdbc/execute! ds ["SELECT * FROM Users WHERE Id=?" user-id])]
             (if (empty? user) nil user))))

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
(defn save-game [game-result is-multi-player]
  (when ds
    (let [winnerId (:users/Id (first (get-user-by-id (:id (:winner game-result)))))
          loserId (:users/Id (first (get-user-by-id (:id (:loser game-result)))))
          score (str (:score (:winner game-result)) " - " (if is-multi-player (:score (:loser game-result)) 0))
          game-types (get-gametypes)
          game-type (if is-multi-player (:multiplayer game-types-enum) (:singleplayer game-types-enum))
          game-typeId (some #(when (= (:gametypes/Name %) game-type) (:gametypes/Id %)) game-types)]
      (when (or winnerId loserId)
        (if (:draw game-result)
        (jdbc/execute! ds
                       ["INSERT INTO Games (UserId1, UserId2, Score, WinnerId, GameTypeId, CreatedAt) 
                                             VALUES (?,?,?,?,?,?)" winnerId loserId score nil game-typeId (current-datetime)])
        (jdbc/execute! ds
                       ["INSERT INTO Games (UserId1, UserId2, Score, WinnerId, GameTypeId, CreatedAt) 
                                             VALUES (?,?,?,?,?,?)" winnerId loserId score winnerId game-typeId (current-datetime)]))))))

;; get leaderboard for user
(defn get-leaderboard [userId]
  (when ds
    (normalize-db-result (sql/query ds ["CALL GetPlayerStats(?)" userId]))))

;; get match-history for user
(defn get-match-history [userId]
  (when ds
    (normalize-db-result (sql/query ds ["CALL GetMatchHistory(?)" userId]))))

;; send friend request
(defn send-friend-request [senderId receiverId]
  (when ds
    (let [data (jdbc/execute! ds ["INSERT INTO Friendships (UserId1, UserId2, Status) VALUES (?,?,?)" 
                                  senderId receiverId (:pending friendship-status)])]
      (if data true nil))))

;; update friend request status
(defn update-friend-request [userId1 userId2 accepted]
  (when ds
    (let [data (jdbc/execute! ds ["UPDATE Friendships SET Status=? WHERE (UserId1=? AND UserId2=?) OR ((UserId1=? AND UserId2=?))" 
                                  (if accepted (:accepted friendship-status) (:declined friendship-status)) 
                                  userId1 userId2 userId2 userId1])]
      (if data true nil))))

;; search for friends
(defn get-available-friend-requests
  ([userId] (get-available-friend-requests userId nil))
  ([userId username]
   (when ds
     (let [declined-status (:declined friendship-status)
           sql-base
           "SELECT * FROM Users u
            WHERE u.Id != ?
              AND u.Id NOT IN (
                SELECT CASE 
                  WHEN f.UserId1 = ? THEN f.UserId2
                  ELSE f.UserId1
                END
                FROM Friendships f
                WHERE (f.UserId1 = ? OR f.UserId2 = ?)
                  AND f.Status != ?
              )"
           [sql params] (if username
                          [(str sql-base " AND u.Username LIKE ? LIMIT 20")
                           [userId userId userId userId declined-status (str "%" username "%")]]
                          [(str sql-base " LIMIT 20")
                           [userId userId userId userId declined-status]])]
       (normalize-db-result (jdbc/query ds (into [sql] params)))))))

;; search for active friend requests
(defn get-pending-friend-requests [userId]
  (when ds
    (let [pending-status (:pending friendship-status)
          sql
          "SELECT u.Id, u.Username
           FROM Friendships f
           JOIN Users u ON u.Id = f.UserId1
           WHERE f.Status = ?
             AND f.UserId2 = ?"
          params [pending-status userId]]
      (jdbc/query ds (into [sql] params)))))