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
  ::home-page-proof-editing
  (fn [db _]
    (:home-page-add-proof-editing db)))


(re-frame/reg-sub
  ::home-page-unlock-proof-editing
  (fn [db _]
    (:home-page-proof-unlock-editing db)))

(re-frame/reg-sub
  ::home-page-unlock-deleted-proof-editing
  (fn [db _]
    (:home-page-deleted-proof-unlock-editing db)))


(re-frame/reg-sub
  ::home-page-gun-proofs
  (fn [db _]
    (:gun-user-proofs db)))

(re-frame/reg-sub
  ::wall-page-gun-proofs
  (fn [db _]
    (:gun-wall-proofs db)))

(re-frame/reg-sub
  ::proof-page-gun-proof
  (fn [db _]
    (:gun-proof db)))


(re-frame/reg-sub
  ::home-page-user
  (fn [db _]
    (:user db)))

