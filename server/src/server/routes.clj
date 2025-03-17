(ns server.routes
  (:require
   [clojure.edn :as edn]
   [compojure.core :refer [defroutes GET POST]]
   [ring.util.response :refer [header response]]
   [server.db.dbBroker :refer [login]]))


(defn response-data [status value]
  (-> (response (pr-str {:status status :value value}))
      (header "Content-Type" "application/edn")))

(defroutes app-routes
   (GET "/" [] "<h1>HELLO</h1>")
   (POST "/login" req
     (let [body (-> req :body slurp edn/read-string)
           username (:username body)
           password (:password body)
           user (login username password)]
       (response-data (if user "success" "unsuccessful") (if user user "Wrong credentials")))))