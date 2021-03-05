(ns cljs-proof-of-date.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [re-frame.core :as re-frame]
    [re-pressed.core :as rp]
    [breaking-point.core :as bp]
    [cljs-proof-of-date.events :as events]
    [cljs-proof-of-date.routes :as routes]
    [cljs-proof-of-date.views :as views]
    [cljs-proof-of-date.config :as config]
    ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/app-routes)
  (re-frame/dispatch-sync [::events/initialize-db])
  ;(re-frame/dispatch-sync [::events/init-gun])
  ;(re-frame/dispatch-sync [::rp/add-keyboard-event-listener "keydown"])
  (re-frame/dispatch-sync [::bp/set-breakpoints
                           {:breakpoints [:mobile
                                          768
                                          :tablet
                                          992
                                          :small-monitor
                                          1200
                                          :large-monitor]
                            :debounce-ms 166}])
  (dev-setup)
  (mount-root))
