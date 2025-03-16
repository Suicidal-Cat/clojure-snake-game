(ns client.api.api-calls
  (:require [clojure.edn :as edn]))

(defn login [username password callback]
  (-> (js/fetch "http://localhost:8080/login"
                (clj->js {:method "POST"
                          :headers {"Content-Type" "application/edn"}
                          :body (pr-str {:username username
                                         :password password})}))
      (.then (fn [response] (.text response)))
      (.then (fn [data]
               (callback (:value (edn/read-string data)))))))