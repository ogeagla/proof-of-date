(ns cljs-proof-of-date.views.proof
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.subs :as subs]
            [re-frame.core :as re-frame]))


(defn- row [elems]
  [re-com/h-box
   :gap "0.5em"
   :children elems])


(defn proof-panel []
  (let [proof (re-frame/subscribe [::subs/proof-page-gun-proof])]
    (if (:user @proof)
      [re-com/v-box
       :gap "0.5em"
       :children [(row [[re-com/title :level :level3 :label "Label:"]
                        [re-com/label :label (:label @proof)]])
                  (row [[re-com/title :level :level3 :label "Timestamp:"]
                        [re-com/label :label (:tx-ts @proof)]])
                  (row [[re-com/title :level :level3 :label "User:"]
                        [re-com/label :label (:user @proof)]])
                  (row [[re-com/title :level :level3 :label "SHA256:"]
                        [re-com/label :label (:proofhash @proof)]])
                  (row [[re-com/title :level :level3 :label "Source text:"]
                        [re-com/label :label (:source-txt @proof)]])]]
      [re-com/title :label "Proof not found" :level :level1])))
