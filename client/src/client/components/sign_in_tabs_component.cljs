(ns client.components.sign-in-tabs-component
  (:require
   [client.api.api-calls :refer [login register]]
   [client.helper-func :as h :refer [set-local-storage get-user-info]]
   [reagent.core :as r]))

(defonce sign-in-tabs-state {:logged (r/atom false)})

(defn update-user-state []
  (let [user (get-user-info)]
    (reset! (:logged sign-in-tabs-state) (some? user))))

(update-user-state)

(defn login-form [callback]
  (when (not @(:logged sign-in-tabs-state)) (let [message (r/atom "")]
                                             [:div
                                              [:h2 "Login"]
                                              [:form {:on-submit (fn [e]
                                                                   (.preventDefault e)
                                                                   (let [form-data (js/FormData. (.-target e))
                                                                         email (.get form-data "email")
                                                                         password (.get form-data "password")]
                                                                     (login email password (fn [result] (when (:user result)
                                                                                                          (set-local-storage "user" (:user result))
                                                                                                          (set-local-storage "token" (:token result))
                                                                                                          (callback))
                                                                                             (update-user-state)))))}
                                               [:div
                                                [:label "Email: "]
                                                [:input {:type "email" :name "email" :required true}]]
                                               [:div
                                                [:label "Password: "]
                                                [:input {:type "password" :name "password" :required true :min-length 8}]]
                                               [:button {:type "submit"} "Login"]]
                                              [:p @message]])))

(defn register-form [switch-callback]
  (when (not @(:logged sign-in-tabs-state))
    (let [message (r/atom "")]
      [:div
       [:h2 "Register"]
       [:form {:on-submit (fn [e]
                            (.preventDefault e)
                            (let [form-data (js/FormData. (.-target e))
                                  email (.get form-data "email")
                                  username (.get form-data "username")
                                  password (.get form-data "password")
                                  form-el (.-target e)]
                              (register email username password
                                        (fn [result] (when result
                                                       (switch-callback)
                                                       (.reset form-el)
                                                       (reset! message ""))))))}
        [:div
         [:label "Email: "]
         [:input {:type "email" :name "email" :required true}]]
        [:div
         [:label "Username: "]
         [:input {:type "text" :name "username" :required true :min-length 4}]]
        [:div
         [:label "Password: "]
         [:input {:type "password" :name "password" :required true :min-length 8}]]
        [:button {:type "submit"} "Register"]]
       [:p @message]])))