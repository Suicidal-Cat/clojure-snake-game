(ns client.components.friends-tab-component
  (:require
   [client.api.api-calls :refer [get-available-friend-requests
                                 get-pending-friend-requests
                                 send-friend-request update-friend-request]]
   [client.components.shared-components :refer [divider spinner]]
   [client.helper-func :refer [user-info]]
   [reagent.core :as r]))

(defonce friends-tab-state {:friends (r/atom nil)
                            :friend-requests (r/atom nil)})


(defn get-available-requests [username]
  (get-available-friend-requests
   (:id @user-info)
   username
   (fn [result]
     (reset! (:friends friends-tab-state) result))))

(defn get-pending-requests []
  (get-pending-friend-requests
   (:id @user-info)
   (fn [result]
     (reset! (:friend-requests friends-tab-state) result))))

(defn friend-request []
  (if (:friends friends-tab-state)
    (let [friends @(:friends friends-tab-state)
          requests @(:friend-requests friends-tab-state)]
      [:div {:class "friends-container"}

       [divider "Add friend"]
       [:form {:class "friends-search"
               :on-submit (fn [e]
                            (.preventDefault e)
                            (let [form (.-target e)
                                  form-data (js/FormData. form)
                                  username (.get form-data "username")]
                              (get-available-requests username)))}
        [:input {:type "text"
                 :name "username"
                 :placeholder "Enter a username..."}]
        [:button {:type "submit"} "Search"]]

       [:div {:class "friends-list"}
        (for [friend friends]
          ^{:key (:id friend)}
          [:div {:class "friends-send-card"}
           [:div {:class "friends-username"} (:username friend)]
           [:button {:class "friends-button"
                     :type "button"
                     :on-click #(send-friend-request
                                 (:id @user-info)
                                 (:id friend)
                                 (fn [_] (get-available-requests nil)))}
            "+"]])]

       [divider "Friend requests"]
       [:div {:class "friend-requests"}
        (for [req requests]
          ^{:key (:id req)}
          [:div {:class "friend-req-card"}
           [:div {:class "friends-username"} (:username req)]
           [:div {:class "friend-req-buttons"}
            [:button {:type "button"
                      :on-click #(update-friend-request
                                  (:id req)
                                  (:id @user-info)
                                  true
                                  (fn [_] (get-pending-requests)))}
             "Accept"]
            [:button {:type "button"
                      :on-click #(update-friend-request
                                  (:id req)
                                  (:id @user-info)
                                  false
                                  (fn [_]
                                    (get-pending-friend-requests
                                     (:id @user-info)
                                     (fn [result]
                                       (reset! (:friend-requests friends-tab-state) result)
                                       (get-available-requests nil)))))}
             "Decline"]]])]])
    [spinner]))