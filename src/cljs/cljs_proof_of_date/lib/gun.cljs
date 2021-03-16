(ns cljs-proof-of-date.lib.gun
  (:require [cljs.core.async :refer [go chan <! >! put! take!]]
            [cljs.core.async.interop :refer-macros [<p!]]
            [cljs-proof-of-date.config :as config]))




(def prod-peer-url
  "https://proofof.date:8765/gun")
(def dev-peer-url
  "http://localhost:8765/gun")


(def prod-server-pubkey
  "eqHVD94wAfe5s1ZGK-Sce8JeGsHcPVgWsr5srnHQ1vU.Bz_Htla8IzLBTFp6HffxoCjJJJew88eusgg3uXoTD2Q")
(def dev-server-pubkey
  "9BsEl4xVr_mstW9AKqoXCztJdEZiC_u-8LtE6OvQVE8.JqwCsiqlm_FkpJcRID2h4dxePJWizip_ggWTUVVKVuM")


(def peer-url
  (if config/debug?
    dev-peer-url
    prod-peer-url))


(def server-pubkey
  (if config/debug?
    dev-server-pubkey
    prod-server-pubkey))


(def gun-prv-user-coll "user-facts")
(def gun-pub-user-coll "facts")
(def gun-pub-user-meta-coll "users")
(def gun-certified-wall-coll "wall-facts")


(def ^js/Gun gun (js/Gun. peer-url))
(def SEA (.-SEA js/Gun))

;; User ops

(def browser-user* (atom nil))
(def app-user* (atom nil))

(defmulti get-user (fn [init-key _]
                     init-key))



(defmethod get-user ::app-user [_ {:keys []}]
  @app-user*)

(defmethod get-user ::browser-user [_ {:keys []}]
  @browser-user*)

(defmethod get-user ::public [_ {:keys []}]
  gun)

(defn- logout-do [u* callback]
  (let [^js/Gun g @u*]
    (when g
      (.leave g #js {} callback)
      (reset! u* nil))))

(defmulti logout (fn [init-key _]
                   init-key))


(defmethod logout ::app-user [_ {:keys [callback]}]
  (logout-do app-user* callback))

(defmethod logout ::browser-user [_ {:keys [callback]}]
  (logout-do browser-user* callback))


(defn- login-do [{:keys [username password callback]}]
  (let [user (-> gun
                 (.user))]
    (.auth user username password callback)
    (reset! app-user* user)
    user))

(defmulti login (fn [init-key _]
                  init-key))


(defmethod login ::browser-user [_ {:keys [username password callback] :as conf}]
  (login-do conf))


(defmethod login ::app-user [_ {:keys [username password callback] :as conf}]
  (login-do conf))


;; TODO critical do signup impls need to reset! their user?
(defmulti signup (fn [init-key _] init-key))

(defmethod signup ::app-user [_ {:keys [username password callback]}]
  (let [user (-> gun
                 (.user))
        _    (->
               user
               (.create username password callback))]
    (reset! app-user* user)
    user))


(defmethod signup ::browser-user [_ {:keys [username password callback]}]
  (let [user (-> gun
                 (.user))
        _    (->
               user
               (.create username password callback))]
    (reset! browser-user* user)
    user))

;; Node ops

(defn- ^js/Gun get-node [{:keys [user path]}]
  (js/console.log "GUN get-node " path)
  (loop [path        path
         ;; This makes it so callers can provide the Gun user or the keyword for the user
         gun-builder (if (keyword? user)
                       (get-user user)
                       user)]
    (if (empty? (rest path))

      ;; this doesn't need await because we need to return the node
      (.get gun-builder (first path))

      (do
        (recur (rest path)
               (.get gun-builder (first path)))))))

(defn get-path-on [{:keys [path user] :as conf}]
  (js/console.log "GUN get-path-on " path)
  (-> (get-node conf)
      (.on)))

(defn map-path-on [{:keys [path callback user] :as conf}]
  (js/console.log "GUN map-path-on " path)
  (-> (get-node conf)
      (.map)
      (.on callback)))


(defn get-path-once [{:keys [path callback user] :as conf}]
  (js/console.log "GUN get-path-once " path)
  (-> (get-node conf)
      (.once callback)))


(defn put-path [{:keys [path callback data user] :as conf}]
  (js/console.log "GUN put-path-once " path)
  (-> (get-node conf)
      (.put (clj->js data) callback)))



(defn set-path [{:keys [user path data callback] :as conf}]
  (js/console.log "GUN set-path " path " -> " data " for user : " user)

  ;; TODO await this also
  (->
    (get-node conf)
    (.set
      (clj->js data)
      callback))
  )


;; SEA

(defn encrypt [{:keys [data secret callback] :as conf}]
  (.encrypt
    SEA
    data
    secret
    callback))

(defn secret [{:keys [epub ^js/SEA pair callback] :as conf}]
  (.secret
    SEA
    epub
    pair
    callback))


(defn sign [{:keys [data ^js/SEA pair callback] :as conf}]
  (.sign
    SEA
    data
    pair
    callback))

(defn get-user-pub [^js/SEA pair]
  (.-pub pair))


(defn get-user-epub [^js/SEA pair]
  (.-epub pair))


(defn get-user-pair [gun-app-user]
  (let [^js/Gun user (if (keyword? gun-app-user)
                       (get-user gun-app-user)
                       gun-app-user)]
    (.-sea (.-_ user))))



;; experiment:
(defn- resolve-prom [^js/Promise p]
  (let [c (chan)]
    (go
      (js/console.log "got put! res: " (<! c)))
    (go
      (let [resp (<p! p)]
        (js/console.log "prom resp: " resp)
        resp
        (>! c resp)
        )))

  #_(-> p
        (.then #(js/console.log %))
        (.catch #(js/console.log %))
        (.finally #(js/console.log "cleanup"))))

;; TODO refactor:
;; x replace .set calls
;; x replace .once calls
;; - replace .on calls
