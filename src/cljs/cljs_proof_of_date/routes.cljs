(ns cljs-proof-of-date.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType])
  (:require
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [re-frame.core :as re-frame]
   [cljs-proof-of-date.lib.gun :as gunlib]
   [re-pressed.core :as rp]
   [cljs-proof-of-date.events :as events]))

(defn hook-browser-navigation! []
  (doto (History.)
    (gevents/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token ^js event))))
    (.setEnabled true)))

(defn app-routes []
  (secretary/set-config! :prefix "#")
  ;; --------------------
  ;; define routes here
  (defroute "/" []
    (re-frame/dispatch-sync [::events/set-active-fact nil])
    (re-frame/dispatch-sync [::events/init-gun-and-users nil false ::gunlib/browser-user [::events/gun-get-browser-facts]])
    (re-frame/dispatch [::events/set-active-panel :home-panel]))


  (defroute "/fact/:key-id" [key-id]
    (js/console.log "Fact route: " key-id)
    (re-frame/dispatch-sync [::events/set-active-fact key-id])
    (re-frame/dispatch-sync [::events/init-gun-and-users key-id false ::gunlib/browser-user [::events/gun-get-browser-facts]])
    (re-frame/dispatch [::events/set-active-panel :fact-panel]))


  (defroute "/user/:username" [username]

    (re-frame/dispatch-sync [::events/home-page-cancel-fact-btn-press false])
    (re-frame/dispatch-sync [::events/set-active-fact nil])
    (re-frame/dispatch-sync [::events/init-gun-and-users nil false ::gunlib/app-user [::events/gun-get-user-facts username]])
    (re-frame/dispatch [::events/set-active-panel :user-panel]))


  ;; --------------------
  (hook-browser-navigation!))


