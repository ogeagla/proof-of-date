(ns cljs-proof-of-date.views.user
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.subs :as subs]
            [cljs-proof-of-date.events :as events]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as string]
            [secretary.core :as secretary]))




(defn- home-title []
  [re-com/title
   :label "User Facts:"
   :level :level3])





(def gun-cols
  [{:id :label :header-label "Label" :row-label-fn :label :width 150 :align "left" :vertical-align "middle" :sort-by true}
   {:id :source-txt :header-label "Source" :row-label-fn :source-txt :width 150 :align "left" :vertical-align "middle"}
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

(defn- clean-label [label]
  (->
    (goog.string/replaceAll label "|" "")
    (string/replace "/" "")))

(defn- create-fact-view [user]
  (let [fact-input-label  (reagent/atom nil)
        fact-input-secret (reagent/atom nil)]
    [re-com/v-box
     :gap "0.5em"
     :children [[re-com/label :label "No | or / allowed in fact label"]
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
                                              user (clean-label @fact-input-label) @fact-input-secret])
                     (re-frame/dispatch-sync [::events/gun-get-user-facts nil nil user]))]
                  [re-com/button
                   :label "cancel"
                   :on-click (fn []
                               (re-frame/dispatch-sync [::events/home-page-cancel-fact-btn-press]))]
                  [re-com/label
                   :label (str (js/Date))]]]]]))

(defn- hide [{:keys [user label source-txt facthash tx-id tx-ts]
              :as   fact}]
  (if source-txt
    [re-com/box
     :size "auto"
     :width "30px"
     :child [re-com/md-icon-button
             :md-icon-name "zmdi-eye-off"
             :on-click
             (fn []
               (re-frame/dispatch-sync [::events/gun-hide-fact fact]))]]
    ""))


(defn- show [{:keys [user label source-txt facthash tx-id tx-ts]
              :as   fact}
             is-editing-unlock-fact]
  (if (and (not source-txt)
           (not is-editing-unlock-fact))
    [re-com/box
     :size "auto"
     :width "30px"
     :child [re-com/md-icon-button
             :md-icon-name "zmdi-eye"
             :on-click
             (fn []
               (js/console.log "show try unlock: " tx-id)
               (re-frame/dispatch-sync [::events/home-page-fact-add-unlock-btn-press tx-id]))]]
    ""))

(defn- show-modal [{:keys [user label source-txt facthash tx-id tx-ts]
                    :as   fact}
                   is-editing-unlock-fact
                   unlock-text]
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
    ""))


(defn- fact-row [{:keys [user label source-txt facthash tx-id tx-ts]
                  :as   fact}

                 ]
  (when (and user label facthash tx-id tx-ts)
    (assoc
      fact
      :delete
      [re-com/md-icon-button
       :md-icon-name "zmdi-delete"
       ;:style {:width "60px"}
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
        ]])))


(defn- table-data [user-facts  ]
  (->>
    user-facts
    (map #(fact-row %  ))
    (remove nil?)
    vec))


(def gun-table* (reagent/atom nil))


(defn- panel []
  (let [user-gun-facts         (re-frame/subscribe [::subs/home-page-gun-facts])
        is-editing-fact        (re-frame/subscribe [::subs/home-page-fact-editing])
        user                   (re-frame/subscribe [::subs/home-page-user])]
    (js/console.log "User facts: " @user-gun-facts)
    (reset! gun-table* (table-data @user-gun-facts  ))

    [re-com/v-box
     :gap "0.75em"
     :children
     [
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
              [home-title]
              [panel]]])
