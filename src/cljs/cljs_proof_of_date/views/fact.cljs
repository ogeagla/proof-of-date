(ns cljs-proof-of-date.views.fact
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.subs :as subs]
            [re-frame.core :as re-frame]
            [qr-encode :as qr]))


(defn- row [elems]
  [re-com/h-box
   :gap "0.5em"
   :width "300px"
   :align :stretch
   :children (vec
               (map (fn [e]
                      [re-com/box
                       :size "auto"
                       :width "70px"
                       :align-self :center
                       :child e
                       ]
                      )
                    elems
                    ))])


(defn fact-panel []
  (let [fact-id        (re-frame/subscribe [::subs/fact-page-gun-fact-id])
        wall-gun-facts (re-frame/subscribe [::subs/wall-page-gun-facts])]
    (let [fact (first (filter
                        (fn [{:keys [path]}]
                          (= path @fact-id))
                        @wall-gun-facts))]
      (if fact
        (do
          (js/console.log "Fact found : " @fact-id " in :path of : " @wall-gun-facts)
          [re-com/v-box
           :gap "0.5em"
           :children [(row [[re-com/title :level :level3 :label "Label:"]
                            [re-com/label :label (:label fact)]])
                      (row [[re-com/title :level :level3 :label "Hashgraph TS:"]
                            [re-com/label :label (:proof-hashgraph-tx-ts fact)]])
                      (row [[re-com/title :level :level3 :label "Hashgraph ID:"]
                            [re-com/label :label (:proof-hashgraph-tx-id fact)]])
                      (row [[re-com/title :level :level3 :label "User:"]
                            [re-com/label :label (:user fact)]])
                      (row [[re-com/title :level :level3 :label "SHA256:"]
                            [re-com/h-box
                             :gap "0.5em"
                             :children [[re-com/label :label (:facthash fact)]
                                        ]]])


                      (row [[re-com/title :level :level3 :label "Source text:"]
                            [re-com/label :label (:source-txt fact)]])
                      (row [[re-com/title :level :level3 :label "Hashgraph URL:"]
                            [re-com/hyperlink-href
                             :label "link"
                             :href (str "https://testnet.dragonglass.me/hedera/transactions/"
                                        (->
                                          (:proof-hashgraph-tx-id fact)
                                          (goog.string/replaceAll "@" "")
                                          (goog.string/replaceAll "." "")

                                          ))]])
                      (row [[re-com/title :level :level3 :label "SHA256 QR:"]
                            [re-com/h-box
                             :gap "0.5em"
                             :children [[:img {:src    (qr
                                                         (:facthash fact)
                                                         #js {:type 10 :size 6 :level "H"})
                                               :width  150
                                               :height 150}]
                                        ]]])


                      ]])
        [re-com/title :label "Searching for fact..." :level :level1]))))
