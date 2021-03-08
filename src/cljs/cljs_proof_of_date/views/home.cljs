(ns cljs-proof-of-date.views.home
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [cljs-proof-of-date.events :as events]
            [cljs-proof-of-date.subs :as subs]
            [reagent.core :as reagent]
            [cljs-proof-of-date.views.wall :as wall-view]))



(defn home-title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :label (str @name)
     :level :level1]))



(defn set-user-view [user-input-username user-input-password]
  [re-com/h-box
   :gap "1em"
   :children
   [[re-com/input-text
     :model user-input-username
     :width "100px"
     :on-change #(reset! user-input-username %)]
    [re-com/input-password
     :model user-input-password
     :width "100px"
     :on-change #(reset! user-input-password %)]]])


(defn action-panel []
  (let [user-input-username (reagent/atom nil)
        user-input-password (reagent/atom nil)
        user (re-frame/subscribe [::subs/home-page-user])
        login-message       (re-frame/subscribe [::subs/login-message])]
    (when-not @user
      [re-com/v-box
       :gap "0.5em"
       :children
       [[re-com/label :label "Enter a username / password: "]
        [set-user-view user-input-username user-input-password]
        [re-com/title :label (or @login-message "") :level :level3]
        [re-com/h-box
         :gap "0.5em"
         :children
         [[re-com/button
           :label "login"
           :on-click
           (fn []
             (js/console.log "log ing: " @user-input-username @user-input-password)
             (re-frame/dispatch-sync [::events/home-page-user-password-login @user-input-username @user-input-password])
             (set! (.-hash js/window.location) (str "#/home/" @user-input-username)))]

          [re-com/button
           :label "signup"
           :on-click
           (fn []
             (js/console.log "signup: " @user-input-username @user-input-password)
             (re-frame/dispatch-sync [::events/home-page-user-password-signup
                                      @user-input-username @user-input-password])
             (set! (.-hash js/window.location) (str "#/home/" @user-input-username)))]]]]])))


(defn home-panel []
  [re-com/v-box
   :gap "1em"
   :children [
              ;[home-title]
              ;[link-to-wall-page]

              [action-panel]
              [wall-view/wall-panel false]]])

