(ns server.routes
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [defroutes POST]]
   [ring.util.response :refer [header response]]
   [server.db.dbBroker :refer [get-leaderboard get-match-history login
                               register]]))


(defn response-data [status value]
  (-> (response (pr-str {:status status :value value}))
      (header "Content-Type" "application/edn")))

(defroutes app-routes
  (POST "/login" req
    (let [body (-> req :body slurp edn/read-string)
          email (:email body)
          password (:password body)
          user (login email password)]
      (response-data (if user "successful" "unsuccessful") (if user user "Wrong credentials"))))
  (POST "/register" req
    (let [body (-> req :body slurp edn/read-string)
          email (:email body)
          username (:username body)
          password (:password body)
          result (register email username password)]
      (response-data (if result "successful" "unsuccessful") (if result true false))))
  (POST "/leaderboard" req
    (let [body (-> req :body slurp edn/read-string)
          userId (:userId body)
          result (get-leaderboard userId)]
      (response-data (if result "successful" "unsuccessful") (if result result false))))
  (POST "/match-history" req
    (let [body (-> req :body slurp edn/read-string)
          userId (:userId body)
          result (get-match-history userId)]
      (response-data (if result "successful" "unsuccessful") (if result result false)))))