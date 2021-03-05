(ns cljs-proof-of-date.views.wall
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.events :as events]
            [cljs-proof-of-date.subs :as subs]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))



(defn wall-title []
  [re-com/title
   :label "This is the Wall Page."
   :level :level1])


(defn link-to-home-page []
  (let [user (re-frame/subscribe [::subs/home-page-user])]
    [re-com/hyperlink-href
     :label "User Facts"
     :href (if @user
             (str "#/home/" @user)
             "#/")]))



(defn text-search-panel []
  (let [search-text            (reagent/atom nil)
        selected-searchterm-id (reagent/atom :user)]
    [re-com/h-box
     :gap "1em"
     :children
     [[re-com/label :label "Search by: "]
      [re-com/single-dropdown
       :choices [{:id :user :label "User"}
                 {:id :facthash :label "Hash"}
                 {:id :label :label "Label"}]
       :model selected-searchterm-id
       :width "100px"
       :max-height "300px"
       :on-change #(reset! selected-searchterm-id %)]
      [re-com/label :label "Search text: "]
      [re-com/input-text
       :model search-text
       :width "100px"
       :on-change (fn [txt]
                    (reset! search-text txt))]
      [re-com/button
       :label "Search"
       :on-click
       (fn []
         (js/console.log "search for: " @search-text " / " @selected-searchterm-id)
         (re-frame/dispatch-sync [::events/http-post-sha256 {:text       @search-text
                                                             :searchterm @selected-searchterm-id}]))]]]))



(def gun-cols
  [{:id :user :header-label "User" :row-label-fn :user :width 100 :align "left" :vertical-align "middle" :sort-by true}
   {:id :fact-url :header-label "URL" :row-label-fn :fact-url :width 100 :align "left" :vertical-align "middle" :sort-by true}
   {:id :label :header-label "Label" :row-label-fn :label :width 100 :align "left" :vertical-align "middle" :sort-by true}
   {:id :source-txt :header-label "Source Text" :row-label-fn :source-txt :width 150 :align "left" :vertical-align "middle" :sort-by true}

   ;{:id :path :header-label "Path" :row-label-fn :path :width 200 :align "left" :vertical-align "middle" :sort-by true}
   {:id :tx-ts :header-label "TxTs" :row-label-fn :tx-ts :width 400 :align "left" :vertical-align "middle" :sort-by true}
   ;{:id :tx-id :header-label "TxID" :row-label-fn :tx-id :width 270 :align "left" :vertical-align "middle" :sort-by true}
   {:id :facthash :header-label "Hash" :row-label-fn :facthash :width 500 :align "left" :vertical-align "middle" :sort-by true}])



(def wall-gun-table* (reagent/atom nil))


(defn table [table*]
  [re-com/simple-v-table
   :model table*
   :columns gun-cols
   :fixed-column-border-color "#333"
   :class "wall-table"
   :row-height 40])


(defn data-panel []
  (let [all-gun-facts (re-frame/subscribe [::subs/wall-page-gun-facts])]

    (js/console.log "gun wall facts: " @all-gun-facts)

    (reset! wall-gun-table* (vec
                              (map
                                #(assoc % :fact-url [re-com/hyperlink-href
                                                      :label "fact"
                                                      :href (str "#/fact/"
                                                                 (goog.string/replaceAll
                                                                   (:path %)
                                                                   "/"
                                                                   "|"))])
                                @all-gun-facts)))
    [re-com/v-box
     :gap "1em"
     :children [[table wall-gun-table*]]]))


(defn wall-panel [showlink?]
  [re-com/v-box
   :gap "1em"
   :children [
              ;[wall-title]

              (when showlink?
                [link-to-home-page])

              #_[text-search-panel]

              [data-panel]]])

