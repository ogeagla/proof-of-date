(ns cljs-proof-of-date.views.user
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.subs :as subs]
            [cljs-proof-of-date.events :as events]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [secretary.core :as secretary]))




(defn- home-title []
  (let [name (re-frame/subscribe [::subs/name])]
    [re-com/title
     :label (str @name)
     :level :level1]))


(defn- link-to-wall-page []
  [re-com/hyperlink-href
   :label "See All"
   :href "#/wall"])


(def gun-cols
  [
   {:id :label :header-label "Label" :row-label-fn :label :width 100 :align "left" :vertical-align "middle" :sort-by true}
   {:id :source-txt :header-label "Source" :row-label-fn :source-txt :width 200 :align "left" :vertical-align "middle"}
   {:id :delete :header-label "Delete" :row-label-fn :delete :width 70 :align "left" :vertical-align "middle"}

   ;{:id :path :header-label "Path" :row-label-fn :path :width 400 :align "left" :vertical-align "middle" :sort-by true}
   {:id :tx-ts :header-label "TxTs" :row-label-fn :tx-ts :width 400 :align "left" :vertical-align "middle" :sort-by true}
   ;{:id :tx-id :header-label "TxID" :row-label-fn :tx-id :width 270 :align "left" :vertical-align "middle" :sort-by true}
   {:id :facthash :header-label "Hash" :row-label-fn :facthash :width 500 :align "left" :vertical-align "middle" :sort-by true}])


(defn- table [table*]
  [re-com/simple-v-table
   :model table*
   :columns gun-cols
   :fixed-column-border-color "#333"
   :row-height 40])


(defn- create-fact-view [user]
  (let [fact-input-label  (reagent/atom nil)
        fact-input-secret (reagent/atom nil)]
    [re-com/h-box
     :gap "1em"
     :children
     [[re-com/label :label "Name: "]
      [re-com/input-text
       :model fact-input-label
       :width "100px"
       :on-change #(reset! fact-input-label %)]
      [re-com/label :label "Content: "]
      [re-com/input-text
       :model fact-input-secret
       :width "100px"
       :on-change #(reset! fact-input-secret %)]

      [re-com/button
       :label "save"
       :on-click
       (fn []

         (re-frame/dispatch-sync [::events/home-page-save-fact-btn-press
                                  user @fact-input-label @fact-input-secret])
         (re-frame/dispatch-sync [::events/gun-get-user-facts user]))]
      [re-com/button
       :label "cancel"
       :on-click (fn []
                   (re-frame/dispatch-sync [::events/home-page-cancel-fact-btn-press]))]
      [re-com/label
       :label (str (js/Date))]]]))


(defn- fact-secret [{:keys [user label source-txt facthash tx-id tx-ts]
                      :as   fact}
                     is-editing-unlock-fact
                     unlock-text]
  (when (and user label facthash tx-id tx-ts)
    (assoc
      fact
      :delete
      [re-com/button
       :label "Delete"
       :style {:width "60px"}
       :on-click
       (fn []
         (re-frame/dispatch-sync [::events/gun-delete-fact fact]))]
      :source-txt
      [re-com/h-box
       :gap "0.25em"
       :align :stretch
       :width "200px"
       :children
       [[re-com/box
         :size "auto"
         :width "70px"
         :align-self :center
         :child (if source-txt
                  [re-com/label :label source-txt]
                  "")]


        (if source-txt
          [re-com/box
           :size "auto"
           :width "60px"
           :child [re-com/button
                   :label "Hide"
                   :style {:width "60px"}
                   :on-click
                   (fn []
                     (re-frame/dispatch-sync [::events/gun-hide-fact fact]))]]
          "")


        (if (and (not source-txt)
                 (not is-editing-unlock-fact))
          [re-com/box
           :size "auto"
           :width "60px"
           :child [re-com/button
                   :label "Show"
                   :style {:width "60px"}
                   :on-click
                   (fn []
                     (js/console.warn "show try unlock: " tx-id)
                     (re-frame/dispatch-sync [::events/home-page-fact-add-unlock-btn-press tx-id]))]]
          "")


        (if (and (not source-txt)
                 is-editing-unlock-fact
                 (= is-editing-unlock-fact tx-id))
          [re-com/modal-panel
           :wrap-nicely? true
           :child
           [re-com/v-box
            :gap "0.75em"
            :children
            [[re-com/label :label facthash]
             [re-com/label :label "Unhide source text?"]
             [re-com/input-text
              :model unlock-text
              :width "200px"
              :on-change #(reset! unlock-text %)]
             [re-com/button
              :label "Cancel"
              :on-click (fn []
                          (re-frame/dispatch-sync
                            [::events/home-page-fact-cancel-unlock-btn-press]))]
             [re-com/button
              :label "Save!"
              :on-click (fn []
                          (re-frame/dispatch-sync
                            [::events/home-page-fact-save-unlock-btn-press
                             user label tx-id @unlock-text facthash]))]]]]
          "")]])))


(defn- table-data [user-facts is-editing-unlock-fact unlock-text]
  (->>
    user-facts
    (map #(fact-secret % is-editing-unlock-fact unlock-text))
    (remove nil?)
    vec))


(def gun-table* (reagent/atom nil))


(defn- panel []
  (let [user-gun-facts         (re-frame/subscribe [::subs/home-page-gun-facts])
        is-editing-fact       (re-frame/subscribe [::subs/home-page-fact-editing])
        user                    (re-frame/subscribe [::subs/home-page-user])
        is-editing-unlock-fact (re-frame/subscribe [::subs/home-page-unlock-fact-editing])
        unlock-text3            (reagent/atom nil)]
    (js/console.log "User facts: " @user-gun-facts)
    (reset! gun-table* (table-data @user-gun-facts @is-editing-unlock-fact unlock-text3))

    [re-com/v-box
     :gap "0.75em"
     :children
     [[re-com/h-box
       :gap "0.75em"
       :children
       [[:label (if @user "Current user: " "") @user]]]
      (if @is-editing-fact
        [create-fact-view @user]
        (if @user
          [re-com/button
           :label "add fact"
           :on-click (fn []
                       (re-frame/dispatch-sync [::events/home-page-add-fact-btn-press]))]
          [:p "Set user to create facts"]))
      [table gun-table*]]]))


(defn user-panel []
  [re-com/v-box
   :gap "1em"
   :children [
              ;[home-title]
              [link-to-wall-page]
              [panel]]])
