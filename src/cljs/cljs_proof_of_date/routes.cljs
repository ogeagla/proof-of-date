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
    (re-frame/dispatch-sync [::events/clear-user-data])
    (re-frame/dispatch-sync [::events/gun-get-all-proofs false])
    (re-frame/dispatch [::events/set-active-panel :home-panel]))

  (defroute "/wall" []
    (re-frame/dispatch-sync [::events/gun-get-all-proofs true])
    (re-frame/dispatch [::events/set-active-panel :wall-panel]))

  (defroute "/proof/:key-id" [key-id]
    (js/console.log "Proof route: " key-id)
    (re-frame/dispatch-sync [::events/gun-get-proof key-id true ])
    (re-frame/dispatch [::events/set-active-panel :proof-panel]))

  (defroute "/home/:username" [username]

    (js/console.log "Home user route: " username)

    (re-frame/dispatch-sync [::events/home-page-set-user username])
    (re-frame/dispatch-sync [::events/gun-get-user-proofs username])
    (re-frame/dispatch [::events/set-active-panel :user-panel])
    )


  ;; --------------------
  (hook-browser-navigation!))
