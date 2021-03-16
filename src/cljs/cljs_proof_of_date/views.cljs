(ns cljs-proof-of-date.views
  (:require
    [re-frame.core :as re-frame]
    [re-com.core :as re-com]
    [re-com.util :as rc-util]
    [cljs-proof-of-date.views.home :as home-view]
    [cljs-proof-of-date.views.user :as user-view]
    [cljs-proof-of-date.views.wall :as wall-view]
    [cljs-proof-of-date.views.fact :as fact-view]
    [cljs-proof-of-date.views.donate :as donate-view]
    [cljs-proof-of-date.events :as events]
    [cljs-proof-of-date.subs :as subs]
    [gun-avatar :as gun-avatar]
    [reagent.core :as reagent]))


(def tabs
  [{:id ::all-facts :label "All Facts" :hash-fn #(str "#/")}
   {:id ::user-facts :label "User Facts" :hash-fn #(str "#/user/" %)}])


(defn- nav-and-logout [panel-name]
  (let [user (re-frame/subscribe [::subs/home-page-user])]
    (when @user
      (let [selected-tab-id (reagent/atom (case panel-name

                                            :home-panel ::all-facts
                                            :user-panel ::user-facts
                                            ::fact))]

        (when-not (= ::fact @selected-tab-id)
          [re-com/v-box
           :gap "2em"
           :children
           [[re-com/horizontal-tabs
             :model selected-tab-id
             :tabs tabs
             :on-change (fn [sel]
                          (js/console.log "tab " sel)
                          (reset! selected-tab-id sel)
                          (set! (.-hash js/window.location)
                                (apply
                                  (:hash-fn (rc-util/item-for-id @selected-tab-id tabs))
                                  [@user])))]

            ;; when I change the pk, the avatar changes, but its still a crappy grey blob
            #_[:img {:src    (gun-avatar/gunAvatar
                               "HPIDWzfLd1JVuXi8kONrVyUucFgLQyq3hx_xP1upUVQ.e2dDxJW0uufD_MKaENiIIERLUjTQEJKva9e66YqO66M"
                               200)
                     :width  "200"
                     :height "200"}]
            [re-com/button
             :label (str "Logout: " @user)
             :on-click
             (fn []
               (js/console.log "log out: " @user)
               (re-frame/dispatch-sync [::events/home-page-user-password-logout]))]
            ]])))))


(defn- panels [panel-name]
  [re-com/v-box
   :gap "1em"
   :children [[nav-and-logout panel-name]
              (case panel-name
                :user-panel [user-view/user-panel]
                :home-panel [home-view/home-panel]
                :wall-panel [wall-view/wall-panel]
                :fact-panel [fact-view/fact-panel]
                [:div])
              [donate-view/view]]])


(defn show-panel [panel-name]
  [panels panel-name])


(defn main-panel []
  (let [active-panel (re-frame/subscribe [::subs/active-panel])]
    [re-com/v-box
     :height "100%"
     :children [[panels @active-panel]]]))

