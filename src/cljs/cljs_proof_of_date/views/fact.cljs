(ns cljs-proof-of-date.views.fact
  (:require [re-com.core :as re-com]
            [cljs-proof-of-date.subs :as subs]
            [re-frame.core :as re-frame]))


(defn- row [elems]
  [re-com/h-box
   :gap "0.5em"
   :children elems])


(defn fact-panel []
  (let [fact (re-frame/subscribe [::subs/fact-page-gun-fact])]
    (if (:user @fact)
      [re-com/v-box
       :gap "0.5em"
       :children [(row [[re-com/title :level :level3 :label "Label:"]
                        [re-com/label :label (:label @fact)]])
                  (row [[re-com/title :level :level3 :label "Timestamp:"]
                        [re-com/label :label (:tx-ts @fact)]])
                  (row [[re-com/title :level :level3 :label "User:"]
                        [re-com/label :label (:user @fact)]])
                  (row [[re-com/title :level :level3 :label "SHA256:"]
                        [re-com/label :label (:facthash @fact)]])
                  (row [[re-com/title :level :level3 :label "Source text:"]
                        [re-com/label :label (:source-txt @fact)]])]]
      [re-com/title :label "Fact not found" :level :level1])))
