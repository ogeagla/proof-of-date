(ns cljs-proof-of-date.views
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :as re-com]
    [cljs-proof-of-date.views.home :as home-view]
    [cljs-proof-of-date.views.user :as user-view]
    [cljs-proof-of-date.views.wall :as wall-view]
    [cljs-proof-of-date.views.fact :as fact-view]
    [cljs-proof-of-date.views.donate :as donate-view]
    [cljs-proof-of-date.events :as events]
    [cljs-proof-of-date.subs :as subs]))


;; main

(defn logout []
  (let [user (re-frame/subscribe [::subs/home-page-user])]
    (when @user
      [re-com/v-box
       :gap "0.5em"
       :children [
                  [re-com/button
                   :label (str "Logout: " @user)
                   :on-click
                   (fn []
                     (js/console.log "log out: " @user)
                     (re-frame/dispatch-sync [::events/home-page-user-password-logout]))]
                  [re-com/hyperlink-href
                   :label "All Facts"
                   :href (str "#/")]
                  [re-com/hyperlink-href
                   :label "User Facts"
                   :href (str "#/home/" @user)]
                  ]])))

(defn- panels [panel-name]
  [re-com/v-box
   :gap "1em"
   :children
   [[donate-view/view]
    [logout]
    (case panel-name
      :user-panel [user-view/user-panel]
      :home-panel [home-view/home-panel]
      :wall-panel [wall-view/wall-panel true]
      :fact-panel [fact-view/fact-panel]
      [:div])]])

(defn show-panel [panel-name]
  [panels panel-name])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :height "100%"
     :children [[panels @active-panel]]]))



