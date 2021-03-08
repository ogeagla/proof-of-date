(ns cljs-proof-of-date.routes
  (:require-macros [secretary.core :refer [defroute]])
  (:import [goog History]
           [goog.history EventType])
  (:require
   [secretary.core :as secretary]
   [goog.events :as gevents]
   [re-frame.core :as re-frame]
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


    (re-frame/dispatch-sync [::events/init-gun-and-users false :gun-browser-user [::events/gun-get-browser-facts]])
    (re-frame/dispatch [::events/set-active-panel :home-panel]))






  ;; TODO make the fact route work again
  ;(defroute "/fact/:key-id" [key-id]
  ;  (js/console.log "Fact route: " key-id)
  ;  (re-frame/dispatch-sync [::events/gun-get-fact key-id true ])
  ;  (re-frame/dispatch [::events/set-active-panel :fact-panel]))



  (defroute "/home/:username" [username]

    (js/console.log "Home user route: " username)
    (re-frame/dispatch-sync [::events/init-gun-and-users false :gun-app-user [::events/gun-get-user-facts username]])
    (re-frame/dispatch [::events/set-active-panel :user-panel])
    )


  ;; --------------------
  (hook-browser-navigation!))




;; TODO: now deploy needs a hokey pokey to copypaste the server PK in app code
