(ns cljs-proof-of-date.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::active-panel
 (fn [db _]
   (:active-panel db)))

(re-frame/reg-sub
 ::login-message
 (fn [db _]
   (:login-message db)))

(re-frame/reg-sub
 ::re-pressed-example
 (fn [db _]
   (:re-pressed-example db)))

(re-frame/reg-sub
  ::home-page-fact-editing
  (fn [db _]
    (:home-page-add-fact-editing db)))


(re-frame/reg-sub
  ::home-page-unlock-fact-editing
  (fn [db _]
    (:home-page-fact-unlock-editing db)))


(re-frame/reg-sub
  ::home-page-gun-facts
  (fn [db _]
    (:gun-user-facts db)))

(re-frame/reg-sub
  ::wall-page-gun-facts
  (fn [db _]
    (:gun-wall-facts db)))

(re-frame/reg-sub
  ::fact-page-gun-fact
  (fn [db _]
    (:gun-fact db)))


(re-frame/reg-sub
  ::home-page-user
  (fn [db _]
    (:user db)))

