(ns client.api.api-calls
  (:require
   [clojure.edn :as edn]))

(defn login [email password callback]
  (-> (js/fetch "http://localhost:8080/login"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:email email
                                         :password password})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

(defn register [email username password callback]
  (-> (js/fetch "http://localhost:8080/register"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:email email
                                         :username username
                                         :password password})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

(defn get-leaderboard [userId callback]
  (-> (js/fetch "http://localhost:8080/leaderboard"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:userId userId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))

(defn get-match-history [userId callback]
  (-> (js/fetch "http://localhost:8080/match-history"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:userId userId})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))