(ns cljs-proof-of-date.widgets.popover
  (:require [re-com.core :as re-com]))


(defn widget [{:keys [anchor body showing? position title]}]
  [re-com/popover-anchor-wrapper
   :position position
   :showing? showing?
   :popover [re-com/popover-content-wrapper
             :close-button? true
             :title title
             :body body]
   :anchor anchor])
