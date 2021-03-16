(ns cljs-proof-of-date.views.donate
  (:require [re-com.core :as re-com]))


(defn view []
  [re-com/v-box
   :gap "1.0em"
   :children [[re-com/hyperlink-href
               :label "Github Source"
               :href "https://github.com/ogeagla/proof-of-date"]
              [re-com/label
               :label "XMR donate address: 85RgL9VdRhkaQ4AzeqUAADNnPJe2RgJgsQ4zD52xpEAd3Wp1EVMBs2xEfcWudujeGgBSuKGD6Cw5oSfpufdxGLwg7iCseCB"]
              [re-com/label
               :label "Build: 0.1.0-SNAPSHOT"]


              ]])
