(ns cljs-proof-of-date.css
  (:require [garden.def :refer [defstyles]]))

(defstyles screen
  [:.rc-simple-v-table-wrapper {:padding "0px !important"}]
  [:body {:background-color "lightgrey"
          :color            "black"}]
  [:.rc-v-table {:background-color "lightgrey"
                 :color            "black"
                 :padding          "0px"}]
  [:.wall-table {:height "20000px"}]
  [:.my-random-class {:color "red"}]
  [:.level1 {}])
