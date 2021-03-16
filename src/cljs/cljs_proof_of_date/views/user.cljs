(ns cljs-proof-of-date.views.user
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.subs :as subs]
            [cljs-proof-of-date.events :as events]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as string]
            [secretary.core :as secretary]
            [cljs-proof-of-date.widgets.popover :as popover]
            [cljs-proof-of-date.lib.digest :as digest]))




(defn- home-title []
  [re-com/title
   :label "User Facts:"
   :level :level3])


(def gun-cols
  [{:id :proof-hashgraph-tx-ts :header-label "Hashgraph TS" :row-label-fn :proof-hashgraph-tx-ts :width 150 :align "left" :vertical-align "middle" :sort-by true}
   {:id :label :header-label "Label" :row-label-fn :label :width 150 :align "left" :vertical-align "middle" :sort-by true}
   {:id :source-txt :header-label "Source Text" :row-label-fn :source-txt :width 150 :align "left" :vertical-align "middle"}
   {:id :proof-hashgraph-explore-url :header-label "Tx Link" :row-label-fn :proof-hashgraph-explore-url :width 70 :align "left" :vertical-align "middle"}
   {:id :proof-hashgraph-tx-id :header-label "Hashgraph ID" :row-label-fn :proof-hashgraph-tx-id :width 220 :align "left" :vertical-align "middle" :sort-by true}
   {:id :delete :header-label "Delete" :row-label-fn :delete :width 70 :align "left" :vertical-align "middle"}
   {:id :facthash :header-label "Hash" :row-label-fn :facthash :width 500 :align "left" :vertical-align "middle" :sort-by true}
   {:id :tx-ts :header-label "Submitted Date" :row-label-fn :tx-ts :width 400 :align "left" :vertical-align "middle" :sort-by true}])


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

(def fact-input-label (reagent/atom nil))
(def fact-input-secret (reagent/atom nil))

(defn- create-fact-view [user]
  [re-com/v-box
   :gap "1.0em"
   :children
   [[re-com/label :label "Add a label and the content you want to prove"]
    [re-com/h-box
     :gap "1em"
     :children
     [[re-com/label :label "Label: "]
      [re-com/input-text
       :model fact-input-label
       :width "100px"
       :on-change #(reset! fact-input-label %)]
      [re-com/label :label "Content: "]
      [re-com/input-text
       :model fact-input-secret
       :width "300px"
       :change-on-blur? false
       :on-change #(reset! fact-input-secret %)]]]
    [re-com/h-box
     :gap "1em"
     :children [[re-com/label :label "Hash: "]
                [re-com/label :label (digest/get-sha256-str @fact-input-secret)]]]
    [re-com/h-box
     :gap "1em"
     :children
     [[re-com/button
       :label "save"
       :on-click
       (fn []
         (when (and @fact-input-label @fact-input-secret)
           (re-frame/dispatch-sync [::events/home-page-save-fact-btn-press
                                    user (clean-label @fact-input-label) @fact-input-secret])
           (re-frame/dispatch-sync [::events/gun-get-user-facts nil nil user])
           (reset! fact-input-label nil)
           (reset! fact-input-secret nil)))]
      [re-com/button
       :label "cancel"
       :on-click (fn []
                   (reset! fact-input-label nil)
                   (reset! fact-input-secret nil)
                   (re-frame/dispatch-sync [::events/home-page-cancel-fact-btn-press]))]]]]])

(defn- hide
  [{:keys [user label source-txt facthash tx-id tx-ts]
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


(defn- show
  [{:keys [user label source-txt facthash tx-id tx-ts]
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

(defn- show-modal
  [{:keys [user label source-txt facthash tx-id tx-ts]
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


(defn- fact-row
  [{:keys [user label source-txt facthash tx-id tx-ts proof-hashgraph-tx-id proof-hashgraph-tx-ts]
    :as   fact}]
  (when (and user label facthash tx-id tx-ts)
    (merge
      (assoc
        fact
        :proof-hashgraph-explore-url
        (when proof-hashgraph-tx-id
          [re-com/hyperlink-href
           :label "link"
           :href (str "https://testnet.dragonglass.me/hedera/transactions/"
                      (->
                        proof-hashgraph-tx-id
                        (goog.string/replaceAll "@" "")
                        (goog.string/replaceAll "." "")))])
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
                    "")]]])
      (when-not proof-hashgraph-tx-ts
        {:proof-hashgraph-explore-url [re-com/throbber :size :smaller]
         :proof-hashgraph-tx-id       [re-com/throbber :size :smaller]
         :proof-hashgraph-tx-ts       [re-com/throbber :size :smaller]}))))


(defn- table-data [user-facts]
  (->>
    user-facts
    (map #(fact-row %))
    (remove nil?)
    vec))


(def gun-table* (reagent/atom nil))


(defn- panel []
  (let [user-gun-facts  (re-frame/subscribe [::subs/home-page-gun-facts])
        is-editing-fact (re-frame/subscribe [::subs/home-page-fact-editing])
        user            (re-frame/subscribe [::subs/home-page-user])]

    (reset! fact-input-label nil)
    (reset! fact-input-secret nil)
    (reset! gun-table* (table-data @user-gun-facts))

    (let [tour (re-com/make-tour [:step1 :step2])]
      [re-com/v-box
       :gap "0.75em"
       :children
       [

        (if @is-editing-fact
          [create-fact-view @user]
          (if @user

            [re-com/h-box
             :gap "1.0em"
             :align :start
             :children
             [[re-com/md-icon-button
               :md-icon-name "zmdi-pin-help"
               :on-click
               (fn []
                 (re-com/start-tour tour))]

              (popover/widget
                {:anchor   [re-com/button
                            :label "add fact"
                            :on-click (fn []
                                        (re-frame/dispatch-sync [::events/home-page-add-fact-btn-press]))]
                 :body     [re-com/v-box
                            :gap "1.0em"
                            :children [[re-com/label :label "Adding a fact includes giving a label and text content.  That content will be hashed with SHA256 and submitted for proofing."]
                                       [re-com/make-tour-nav tour]]]
                 :showing? (:step1 tour)
                 :position :above-center
                 :title    "Add A Fact"})]]


            [:p "Set user to create facts"]))

        [home-title]
        [re-com/h-box
         :gap "1.0em"
         :align :start
         :children
         [

          (popover/widget
            {:anchor   [table gun-table*]
             :body     [re-com/v-box
                        :gap "1.0em"
                        :children
                        [[re-com/label :label "After adding a fact, it will be shown in this table.  After proofing, the columns will be updated with the relevant values.  Be careful deleting, you cannot undo a delete."]
                         [re-com/make-tour-nav tour]]]
             :showing? (:step2 tour)
             :position :above-center
             :title    "Your User Facts"})]]

        ]])))


(defn user-panel []
  [re-com/v-box
   :gap "1em"
   :children [
              [panel]]])
