(ns client.api.api-calls
  (:require
   [client.helper-func :refer [get-user-token]]
   [clojure.edn :as edn]))

(defn auth-headers []
  #js {"Content-Type"  "application/edn"
       "Authorization" (str "Bearer " (get-user-token))})

;; send login request
(defn login [email password callback]
  (-> (js/fetch "http://localhost:8085/login"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:email email
                                         :password password})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; register user
(defn register [email username password callback]
  (-> (js/fetch "http://localhost:8085/register"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:email email
                                         :username username
                                         :password password})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get latest leaderboard data
(defn get-leaderboard [userId callback]
  (-> (js/fetch "http://localhost:8085/leaderboard"
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get latest matches for user
(defn get-match-history [userId callback]
  (-> (js/fetch "http://localhost:8085/match-history"
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; send friend request
(defn send-friend-request [senderId receiverId callback]
  (-> (js/fetch "http://localhost:8085/friendlist/send"
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:senderId senderId :receiverId receiverId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; update friend request
(defn update-friend-request [userId1 userId2 accepted callback]
  (-> (js/fetch "http://localhost:8085/friendlist/update-status"
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId1 userId1 :userId2 userId2 :accepted accepted})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get avialable users to send request
(defn get-available-friend-requests [userId username callback]
  (-> (js/fetch "http://localhost:8085/friendlist/available-friend-requests"
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId :username username})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get pending friend request for user
(defn get-pending-friend-requests [userId callback]
  (-> (js/fetch "http://localhost:8085/friendlist/pending-friend-requests"
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))