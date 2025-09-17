(ns client.api.api-calls
  (:require
   [client.helper-func :refer [api-domain get-user-token]]
   [clojure.edn :as edn]))

(defn auth-headers []
  #js {"Content-Type"  "application/edn"
       "Authorization" (str "Bearer " (get-user-token))})

(def base-full-domain (str "http://" api-domain "/"))

;; send login request
(defn login [email password callback]
  (-> (js/fetch (str base-full-domain "login")
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:email email
                                         :password password})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; register user
(defn register [email username password callback]
  (-> (js/fetch (str base-full-domain "register")
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:email email
                                         :username username
                                         :password password})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get latest leaderboard data
(defn get-leaderboard [userId isFriends callback]
  (-> (js/fetch (str base-full-domain "leaderboard")
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId :friends isFriends})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get latest matches for user
(defn get-match-history [userId callback]
  (-> (js/fetch (str base-full-domain "match-history")
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; send friend request
(defn send-friend-request [senderId receiverId callback]
  (-> (js/fetch (str base-full-domain "friendlist/send")
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:senderId senderId :receiverId receiverId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; update friend request
(defn update-friend-request [userId1 userId2 accepted callback]
  (-> (js/fetch (str base-full-domain "friendlist/update-status")
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId1 userId1 :userId2 userId2 :accepted accepted})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get avialable users to send request
(defn get-available-friend-requests [userId username callback]
  (-> (js/fetch (str base-full-domain "friendlist/available-friend-requests")
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId :username username})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

;; get pending friend request for user
(defn get-pending-friend-requests [userId callback]
  (-> (js/fetch (str base-full-domain "friendlist/pending-friend-requests")
                (clj->js {:method "POST"
                          :headers (auth-headers)
                          :body (pr-str {:userId userId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))