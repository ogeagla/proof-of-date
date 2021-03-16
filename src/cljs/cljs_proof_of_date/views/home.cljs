(ns cljs-proof-of-date.views.home
  (:require [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [cljs-proof-of-date.events :as events]
            [cljs-proof-of-date.widgets.popover :as popover]
            [cljs-proof-of-date.subs :as subs]
            [reagent.core :as reagent]
            [cljs-proof-of-date.views.wall :as wall-view]))



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


(defn- signup-btn [user-input-username user-input-password]
  [re-com/button
   :label "signup"
   :on-click
   (fn []
     (js/console.log "signup: " @user-input-username @user-input-password)
     (re-frame/dispatch-sync [::events/home-page-user-password-signup
                              @user-input-username @user-input-password])
     (set! (.-hash js/window.location) (str "#/user/" @user-input-username)))])


(defn- signin-btn [user-input-username user-input-password]
  [re-com/button
   :label "login"
   :on-click
   (fn []
     (js/console.log "log ing: " @user-input-username @user-input-password)
     (re-frame/dispatch-sync [::events/home-page-user-password-login @user-input-username @user-input-password])
     (set! (.-hash js/window.location) (str "#/user/" @user-input-username)))])


(defn account-panel []
  (let [user-input-username (reagent/atom nil)
        user-input-password (reagent/atom nil)
        user                (re-frame/subscribe [::subs/home-page-user])
        login-message       (re-frame/subscribe [::subs/login-message])]

    (when-not @user
      (let [tour (re-com/make-tour [:step1 :step2])]
        [re-com/v-box
         :gap "0.5em"
         :children
         [[re-com/h-box
           :gap "1.0em"
           :children [[re-com/md-icon-button
                       :md-icon-name "zmdi-pin-help"
                       :on-click
                       (fn []
                         (re-com/start-tour tour))]
                      [re-com/label :label "Enter a username / password: "]]]
          [set-user-view user-input-username user-input-password]
          [re-com/title :label (or @login-message "") :level :level3]
          [re-com/h-box
           :gap "0.5em"
           :children
           [[re-com/h-box
             :gap "1.0em"
             :align :start
             :children
             [(popover/widget
                {:anchor   (signin-btn user-input-username user-input-password)
                 :body     [re-com/v-box
                            :gap "1.0em"
                            :children
                            [[re-com/label :label "Sign in with an existing username and password."]
                             [re-com/make-tour-nav tour]]]
                 :showing? (:step1 tour)
                 :position :above-right
                 :title    "Sign In"})]]

            [re-com/h-box
             :gap "1.0em"
             :align :start
             :children
             [(popover/widget
                {:anchor   (signup-btn user-input-username user-input-password)
                 :body     [re-com/v-box
                            :gap "1.0em"
                            :children
                            [[re-com/label :label "Sign up with a unique username and password.  After signup, you will then be logged in."]
                             [re-com/make-tour-nav tour]]]
                 :showing? (:step2 tour)
                 :position :above-right
                 :title    "Sign Up"})]]]]]]))))


(defn home-panel []
  [re-com/v-box
   :gap "0.0em"
   :children [[account-panel]
              [wall-view/wall-panel]]])

