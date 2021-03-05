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
   {:id :proofhash :header-label "Hash" :row-label-fn :proofhash :width 500 :align "left" :vertical-align "middle" :sort-by true}])


(defn- table [table*]
  [re-com/simple-v-table
   :model table*
   :columns gun-cols
   :fixed-column-border-color "#333"
   :row-height 40])


(defn- create-proof-view [user]
  (let [proof-input-label  (reagent/atom nil)
        proof-input-secret (reagent/atom nil)]
    [re-com/h-box
     :gap "1em"
     :children
     [[re-com/label :label "Name: "]
      [re-com/input-text
       :model proof-input-label
       :width "100px"
       :on-change #(reset! proof-input-label %)]
      [re-com/label :label "Content: "]
      [re-com/input-text
       :model proof-input-secret
       :width "100px"
       :on-change #(reset! proof-input-secret %)]

      [re-com/button
       :label "save"
       :on-click
       (fn []

         (re-frame/dispatch-sync [::events/home-page-save-proof-btn-press
                                  user @proof-input-label @proof-input-secret])
         (re-frame/dispatch-sync [::events/gun-get-user-proofs user]))]
      [re-com/button
       :label "cancel"
       :on-click (fn []
                   (re-frame/dispatch-sync [::events/home-page-cancel-proof-btn-press]))]
      [re-com/label
       :label (str (js/Date))]]]))


(defn- proof-secret [{:keys [user label source-txt proofhash tx-id tx-ts]
                      :as   proof}
                     is-editing-unlock-proof
                     unlock-text]
  (when (and user label proofhash tx-id tx-ts)
    (assoc
      proof
      :delete
      [re-com/button
       :label "Delete"
       :style {:width "60px"}
       :on-click
       (fn []
         (re-frame/dispatch-sync [::events/gun-delete-proof proof]))]
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
                     (re-frame/dispatch-sync [::events/gun-hide-proof proof]))]]
          "")


        (if (and (not source-txt)
                 (not is-editing-unlock-proof))
          [re-com/box
           :size "auto"
           :width "60px"
           :child [re-com/button
                   :label "Show"
                   :style {:width "60px"}
                   :on-click
                   (fn []
                     (js/console.warn "show try unlock: " tx-id)
                     (re-frame/dispatch-sync [::events/home-page-proof-add-unlock-btn-press tx-id]))]]
          "")


        (if (and (not source-txt)
                 is-editing-unlock-proof
                 (= is-editing-unlock-proof tx-id))
          [re-com/modal-panel
           :wrap-nicely? true
           :child
           [re-com/v-box
            :gap "0.75em"
            :children
            [[re-com/label :label proofhash]
             [re-com/label :label "Unhide source text?"]
             [re-com/input-text
              :model unlock-text
              :width "200px"
              :on-change #(reset! unlock-text %)]
             [re-com/button
              :label "Cancel"
              :on-click (fn []
                          (re-frame/dispatch-sync
                            [::events/home-page-proof-cancel-unlock-btn-press]))]
             [re-com/button
              :label "Save!"
              :on-click (fn []
                          (re-frame/dispatch-sync
                            [::events/home-page-proof-save-unlock-btn-press
                             user label tx-id @unlock-text proofhash]))]]]]
          "")]])))


(defn- table-data [user-proofs is-editing-unlock-proof unlock-text]
  (->>
    user-proofs
    (map #(proof-secret % is-editing-unlock-proof unlock-text))
    (remove nil?)
    vec))


(def gun-table* (reagent/atom nil))


(defn- panel []
  (let [user-gun-proofs         (re-frame/subscribe [::subs/home-page-gun-proofs])
        is-editing-proof        (re-frame/subscribe [::subs/home-page-proof-editing])
        user                    (re-frame/subscribe [::subs/home-page-user])
        is-editing-unlock-proof (re-frame/subscribe [::subs/home-page-unlock-proof-editing])
        unlock-text3            (reagent/atom nil)]
    (js/console.log "User proofs: " @user-gun-proofs)
    (reset! gun-table* (table-data @user-gun-proofs @is-editing-unlock-proof unlock-text3))

    [re-com/v-box
     :gap "0.75em"
     :children
     [[re-com/h-box
       :gap "0.75em"
       :children
       [[:label (if @user "Current user: " "") @user]]]
      (if @is-editing-proof
        [create-proof-view @user]
        (if @user
          [re-com/button
           :label "add proof"
           :on-click (fn []
                       (re-frame/dispatch-sync [::events/home-page-add-proof-btn-press]))]
          [:p "Set user to create proofs"]))
      [table gun-table*]]]))


(defn user-panel []
  [re-com/v-box
   :gap "1em"
   :children [
              ;[home-title]
              [link-to-wall-page]
              [panel]]])
