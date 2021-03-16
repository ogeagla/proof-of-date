(ns cljs-proof-of-date.views.wall
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.events :as events]
            [cljs-proof-of-date.subs :as subs]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [clojure.string :as string]
            [cljs-proof-of-date.widgets.popover :as popover]))



(defn wall-title []
  [re-com/title
   :label "All Facts:"
   :level :level3])


(defn wall-search []
  (let [search-txt? (re-frame/subscribe [::subs/search-txt])]
    (js/console.log "search widget: " @search-txt?)
    [re-com/h-box
     :gap "0.5em"
     :children [[re-com/label
                 :label "Search for users, labels, or source text content:"]
                [re-com/input-text
                 :model @search-txt?
                 :change-on-blur? false
                 :on-change (fn [txt]
                              ;(reset! search-txt txt)
                              (js/console.log "change txt: " txt)
                              (re-frame/dispatch-sync [::events/fact-search txt]))]]]))


(defn link-to-home-page []
  (let [user (re-frame/subscribe [::subs/home-page-user])]
    [re-com/hyperlink-href
     :label "User Facts"
     :href (if @user
             (str "#/user/" @user)
             "#/")]))




(def gun-cols
  [{:id :proof-hashgraph-tx-ts :header-label "Hashgraph TS" :row-label-fn :proof-hashgraph-tx-ts :width 150 :align "left" :vertical-align "middle" :sort-by true}
   {:id :user :header-label "User" :row-label-fn :user :width 100 :align "left" :vertical-align "middle" :sort-by true}
   {:id :fact-url :header-label "Fact Link" :row-label-fn :fact-url :width 80 :align "left" :vertical-align "middle"}
   {:id :label :header-label "Label" :row-label-fn :label :width 150 :align "left" :vertical-align "middle" :sort-by true}
   {:id :source-txt :header-label "Source Text" :row-label-fn :source-txt :width 150 :align "left" :vertical-align "middle" :sort-by true}
   {:id :proof-hashgraph-explore-url :header-label "Tx Link" :row-label-fn :proof-hashgraph-explore-url :width 70 :align "left" :vertical-align "middle"}
   {:id :proof-hashgraph-tx-id :header-label "Hashgraph ID" :row-label-fn :proof-hashgraph-tx-id :width 220 :align "left" :vertical-align "middle" :sort-by true}
   {:id :facthash :header-label "Hash" :row-label-fn :facthash :width 500 :align "left" :vertical-align "middle" :sort-by true}])



(def wall-gun-table* (reagent/atom nil))


(defn table [table*]
  [re-com/simple-v-table
   :model table*
   :columns gun-cols
   :fixed-column-border-color "#333"
   :row-height 40])

(defn table-widget [tour]
  (let [wall-gun-facts (re-frame/subscribe [::subs/wall-page-gun-facts])]

    (reset!
      wall-gun-table*
      (vec
        (map
          #(assoc %
             :signature (or (:signature %) "not found")
             :fact-url [re-com/hyperlink-href
                        :label "link"
                        :href (str "#/fact/"
                                   (:path %))]
             :proof-hashgraph-explore-url
             (when (:proof-hashgraph-tx-id %)
               [re-com/hyperlink-href
                :label "link"
                :href (str "https://testnet.dragonglass.me/hedera/transactions/"
                           (->
                             (:proof-hashgraph-tx-id %)
                             (goog.string/replaceAll "@" "")
                             (goog.string/replaceAll "." "")))]))
          @wall-gun-facts)))

    [re-com/h-box
     :gap "1.0em"
     :align :start
     :children
     [(popover/widget
        {:anchor   [table wall-gun-table*]
         :body     [re-com/v-box
                    :gap "1.0em"
                    :children
                    [[re-com/label :label
                      "All proofed facts are displayed here.  Each row links to the proof tx and a dedicated site for the fact that can be linked."]
                     [re-com/make-tour-nav tour]]]
         :showing? (:step1 tour)
         :position :above-center
         :title    "All Facts"})]]))


(defn data-panel []
  (let [user (re-frame/subscribe [::subs/home-page-user])
        tour (re-com/make-tour [:step1])]

    [re-com/v-box
     :gap "0.5em"
     :children
     [(when @user
        [re-com/md-icon-button
         :md-icon-name "zmdi-pin-help"
         :on-click
         (fn []
           (re-com/start-tour tour))])
      [wall-title]
      [wall-search]
      [table-widget tour]]]))


(defn wall-panel []
  [data-panel])

