(ns cljs-proof-of-date.css
  (:require [garden.def :refer [defstyles]]))



;; palette is top all time from: https://colorhunt.co/palettes/popular

(def pallete-1a
  {:primary   "#222831"
   :secondary "#393e46"
   :accent-1  "#00adb5"
   :accent-2  "#eeeeee"
   })


;; NICE, like 1 but brighter accent1
(def pallete-1b
  {
   :primary   "#232931"
   :secondary "#393e46"
   :accent-1  "#4ecca3"
   :accent-2  "#eeeeee"
   })

;; NICE
(def pallete-2
  {
   ;:primary   "#48466d"
   :primary   "#3d84a8"
   ;:secondary "#3d84a8"
   :secondary "#48466d"
   :accent-1  "#46cdcf"
   :accent-2  "#abedd8"
   })

(def pallete-3
  {
   ;:primary   "#303841"
   :primary   "#00adb5"
   ;:secondary "#00adb5"
   :secondary "#303841"
   ;:accent-1  "#eeeeee"
   :accent-1  "#ff5722"
   ;:accent-2  "#ff5722"
   :accent-2  "#eeeeee"
   })


(def pallete-4
  {
   ;:primary   "#364f6b"
   :primary   "#3fc1c9"
   ;:secondary "#3fc1c9"
   :secondary "#364f6b"
   ;:accent-1  "#f5f5f5"
   :accent-1  "#fc5185"
   ;:accent-2  "#fc5185"
   :accent-2  "#f5f5f5"
   })

(def pallete-5
  {
   ;:primary   "#cefff1"
   :primary   "#ace7ef"
   ;:secondary "#ace7ef"
   :secondary "#cefff1"
   :accent-1  "#a6acec"
   :accent-2  "#a56cc1"
   })



;; NICE:
(def pallete-6
  {
   :primary   "#00204a"
   :secondary "#005792"
   :accent-1  "#00bbf0"
   :accent-2  "#d9faff"
   })

(def pallete-7
  {
   :primary   "#a9eee6"
   :secondary "#fefaec"
   :accent-1  "#f38181"
   :accent-2  "#625772"
   })

(def pallete-8
  {
   ;:primary   "#233142"
   :primary   "#455d7a"
   ;:secondary "#455d7a"
   :secondary "#233142"
   :accent-1  "#f95959"
   :accent-2  "#e3e3e3"
   })

(def pallete-9
  {
   ;:primary   "#f2f2f2"
   :primary   "#ebd5d5"
   ;:secondary "#ebd5d5"
   :secondary  "#f2f2f2"
   :accent-1  "#ea8a8a"
   :accent-2  "#685454"
   })

(def pallete-10
  {
   ;:primary   "#fbfbfb"
   :primary   "#b9e1dc"
   ;:secondary "#b9e1dc"
   :secondary "#fbfbfb"
   :accent-1  "#f38181"
   :accent-2  "#756c83"
   })


;; NICE:
(def pallete-11
  {
   ;:primary   "#f9f7f7"
   :primary   "#dbe2ef"
   ;:secondary "#dbe2ef"
   :secondary "#f9f7f7"
   :accent-1  "#3f72af"
   :accent-2  "#112d4e"
   })

(def pallete-12
  {
   ;:primary   "#fcf8e8"
   :primary   "#d4e2d4"
   ;:secondary "#d4e2d4"
   :secondary "#fcf8e8"
   :accent-1  "#ecb390"
   :accent-2  "#df7861"
   })





;; NICE
(def pallete-15
  {
   :primary   "#f6f5f5"
   :secondary "#d3e0ea"
   :accent-1  "#1687a7"
   :accent-2  "#276678"
   })

;; NICE
(def pallete-16
  {
   :primary   "#0a043c"
   :secondary "#03506f"
   :accent-1  "#a3ddcb"
   :accent-2  "#ffe3de"
   })

;; NICE?
(def pallete-17
  {
   :primary   "#312c51"
   :secondary "#48426d"
   :accent-1  "#f0c38e"
   :accent-2  "#f1aa9b"
   })

;; NICE
(def pallete-18
  {
   :primary   "#493323"
   :secondary "#91684a"
   :accent-1  "#eaac7f"
   :accent-2  "#ffdf91"
   })

;; NICE
(def pallete-19
  {
   :primary   "#e7e6e1"
   :secondary "#f7f6e7"
   :accent-1  "#f2a154"
   :accent-2  "#314e52"
   })

(def app-palette pallete-16)

(def color-1 (:primary app-palette))
(def color-2 (:secondary app-palette))
(def color-3 (:accent-1 app-palette))
(def color-4 (:accent-2 app-palette))


(defstyles screen

  [:.rc-simple-v-table-wrapper
   {:padding         "10px !important"
    :background-clip "content-box"
    :box-shadow      (str "inset 0 0 0 10px " color-2)}]

  [:body
   {:background-color color-1
    :color            color-4}]

  [:.rc-simple-v-table-column-header
   {
    :background-color color-2
    ;:color           color-2
    }]

  [:path
   {:stroke color-4
    :fill   color-4}]

  [:a
   {:color color-3}]

  [:.rc-tabs
   {:background-color color-1}]

  [:.nav-tabs>li.active>a
   {:background-color color-1
    :color color-3 }]

  [:.nav-tabs>li.active>a:hover
   {:background-color color-2
    :color color-3 }]

  [:.nav-tabs>li>a:hover
   {:background-color color-2
    :color color-3 }]

  [:input
   {:color            (str color-3 " !important")
    :background-color (str color-2 " !important")}]


  [:.popover-content
   {:background-color color-2
    :color            color-4}]

  [:.popover-title
   {:background-color color-2
    :color            color-4}]

  [:.level3
   {:color color-4}]

  [:.level2
   {:color color-4}]

  [:.level1
   {:color color-4}]

  [:.rc-v-table
   {:background-color color-2
    :color            color-4
    :padding          "0px"}]

  [:.rc-button:hover
   {:background-color color-3
    :color            color-2}]

  [:.rc-button:disabled
   {:background-color color-1
    :color            color-3}]

  [:.rc-button
   {:background-color color-2
    :color            color-3}])
