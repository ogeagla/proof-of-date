(ns cljs-proof-of-date.events
  (:require
    [re-frame.core :as re-frame]
    [cljs-proof-of-date.db :as db]
    [cljs-proof-of-date.digest :as digest]
    [goog.crypt :as crypt]
    [ajax.core :as ajax]
    [day8.re-frame.tracing :refer-macros [fn-traced]]
    [clojure.string :as string]
    [cljs-proof-of-date.config :as config])
  (:import goog.crypt.Sha256))

(def prod-peer-url "https://proofof.date:8765/gun")
(def peer-url
  (if config/debug?
    "http://localhost:8765/gun"
    prod-peer-url))
;(def gun-user-coll "test-user-facts")

;;site:
(def prod-server-pk "Xrc5phui0oDOREdSE-5gEQavg-uBm_xR6xPn6rst4uU.QINSFGT8jYaek9th_iHTxqYDGV5i52WVMFIFyN_jLmk")

;; dev:
(def server-pk
  (if config/debug?
    "yZLdDEpltoqDR0fovOMyrCJdAUEK_0FW7lV6-w8jiL4.dlvq0O1op7LH3NIFXFmkPO3axuujxqNezz19uyoDjvM"
    prod-server-pk))


(def gun-v2-prv-user-coll "user-factsv2")
(def gun-v2-pub-user-coll "factsv2")
(def gun-v2-certified-wall-coll "wall-factsv2")


(defn nav-to [nav-hash]
  (js/console.log "NAV TO " nav-hash)
  (set! (.-hash js/window.location) nav-hash))

(re-frame/reg-event-db
  ::initialize-db
  (fn-traced [_ _]
    db/default-db))

(re-frame/reg-event-db
  ::set-active-panel
  (fn-traced [db [_ active-panel]]
    (assoc db :active-panel active-panel)))

(re-frame/reg-event-db
  ::set-re-pressed-example
  (fn [db [_ value]]
    (assoc db :re-pressed-example value)))


(re-frame/reg-event-fx
  ::home-page-add-fact-btn-press
  (fn [{:keys [db]} _]
    {:db (assoc db :home-page-add-fact-editing true)}))


(re-frame/reg-event-fx
  ::home-page-fact-add-unlock-btn-press
  (fn [{:keys [db]} [_ tx-id]]
    {:db (assoc db :home-page-fact-unlock-editing tx-id)}))


(defn ^:export got-user-leave [d]
  (js/console.log "User leave result: " d))


(re-frame/reg-event-fx
  ::home-page-user-password-logout
  (fn [{:keys [db]} [_]]
    (nav-to "#/")
    {:db (dissoc db :current-username :current-password)}))




(re-frame/reg-event-fx
  ::home-page-user-password-signup-failure
  (fn [{:keys [db]} [_ username reason]]
    (let []

      {:db (assoc db :login-message (str "Failed to sign up " username " , Reason: " reason))})))


(defn ^:export got-user-signup [gun-user username password ^js/Object e]
  (if (.-err e)
    (do
      (js/console.error "Create user error " (.-err e))
      (js/alert (str "Create error: " (.-err e)))

      ;; TODO if (.-err e) is user already created, then call auth

      (re-frame/dispatch [::home-page-user-password-signup-failure username (.-err e)]))
    (do
      (js/console.log "Created user! " e " , gun: " gun-user)
      (re-frame/dispatch [::home-page-user-password-login username password])
      (nav-to (str "#/home/" username))
      false)))


(re-frame/reg-event-fx
  ::home-page-user-password-signup
  (fn [{:keys [db]} [_ username password]]
    {:db (dissoc db :login-message
                 :current-username username
                 :current-password password)
     :fx [[:dispatch [::init-gun-and-users
                      [username password] :gun-app-user [::gun-get-user-facts username]]]]}))


(defn ^:export got-user-fact [username data key]
  (re-frame/dispatch [::gun-got-user-fact username key data]))



(re-frame/reg-event-fx
  ::gun-get-user-facts
  (fn [{:keys [db]} [_ ^js/Gun gun-app-user ^js/Gun gun-lib username]]

    (js/console.log "::gun-get-user-facts: " username)
    (let [^js/Gun gun-app-user (or gun-app-user (:gun-app-user db))]
      (do (-> (.get gun-app-user gun-v2-prv-user-coll)
              (.map)
              (.not (fn [^js/Object d] (js/console.error "gun get facts nf: " d)))
              (.on
                (partial got-user-fact username)))))
    {:db (assoc db :user username
                   :gun-user-facts [])}))


(re-frame/reg-event-fx
  ::gun-got-user-fact
  (fn [{:keys [db]} [_ username key data]]
    (let [data-map   (js->clj data)
          timestamps (str (js/Date.
                            (js/parseInt
                              (str (get-in data-map ["_" ">" "label"])))))
          path       (str (get-in data-map ["_" "#"]))
          old-map    (:gun-user-facts db)
          d          {:recv-acct-id "recv-acct-id"
                      :send-acct-id "send-acct-id"
                      :label        (get data-map "label")
                      :user         username
                      :source-txt   (get data-map "secret")
                      :facthash     (get data-map "digest")
                      :path         path
                      :tx-id        key
                      :tx-ts        timestamps}
          new-map    (vec
                       (set (concat [d]
                                    (remove #(= path (:path %)) old-map))))]
      {:db (assoc db :gun-user-facts new-map)})))


(defn ^:export get-user-facts [user]
  (re-frame/dispatch [::gun-get-user-facts nil nil user]))

(defn- get-fact-from-db [db-facts match-user
                         match-hash
                         match-label]
  (->> db-facts
       (filter (fn [{:keys [user facthash label]}]
                 (and (= user match-user)
                      (= facthash match-hash)
                      (= label match-label))))
       first))

(re-frame/reg-event-fx
  ::gun-delete-fact
  (fn [{:keys [db]} [_ fact]]

    (js/console.log "Delete fact: " fact)

    (let [^js/Gun gun-lib      (:gun-lib db)
          ^js/Gun gun-app-user (:gun-app-user db)

          {:keys [source-txt label facthash user tx-id]} fact
          user-data            (get-fact-from-db (:gun-user-facts db) user
                                                 facthash
                                                 label)
          wall-data            (get-fact-from-db (:gun-wall-facts db) user
                                                 facthash
                                                 label)
          path                 (:path user-data)
          splits               (string/split path "/")

          path                 (if (= 1 (count splits))
                                 path
                                 (last splits)
                                 )
          _                    (js/console.log "%%% delete: " path " splits: " splits)
          _                    (-> (.get gun-app-user gun-v2-prv-user-coll)
                                   (.get path)
                                   (.put nil (partial get-user-facts user)))

          _                    (when wall-data
                                 (-> (.get gun-lib gun-v2-pub-user-coll)
                                     (.get (:path wall-data))
                                     (.put nil)))]

      {:db db})))








(re-frame/reg-event-fx
  ::home-page-fact-cancel-unlock-btn-press
  (fn [{:keys [db]} _]
    {:db (assoc db :home-page-fact-unlock-editing false)}))





(re-frame/reg-event-fx
  ::home-page-cancel-fact-btn-press
  (fn [{:keys [db]} _]
    {:db (assoc db :home-page-add-fact-editing false)}))





(re-frame/reg-event-fx
  ::home-page-save-fact-btn-press

  (fn [{:keys [db]} [_ user label secret]]
    (let [^js/Gun gun-lib          (:gun-lib db)
          ^js/Gun gun-app-user     (:gun-app-user db)
          ^js/Gun gun-browser-user (:gun-browser-user db)
          digest                   (digest/get-sha256-str secret)

          _                        (js/console.log "Saved fact has digest data: " user " : " label " : " digest)

          _                        (-> (.get gun-app-user gun-v2-prv-user-coll)
                                       (.set #js {:label    label
                                                  :secret   secret
                                                  :digest   digest
                                                  :username user}))




          _                        (-> (.get gun-lib gun-v2-pub-user-coll)
                                       (.set #js {:label    label
                                                  :secret   secret
                                                  :digest   digest
                                                  :username user}))

          ]
      {:db (assoc db :home-page-add-fact-editing false
                     )})))



(re-frame/reg-event-fx
  ::home-page-user-password-login
  (fn [{:keys [db]} [_ username password]]
    {:db (assoc db
           :current-username username
           :current-password password)}))



(re-frame/reg-event-fx
  ::init-gun-and-users
  (fn [{:keys [db]} [_ signup? which-user next-evt]]
    (js/console.log "$$$ init gun and user: " which-user)
    (let [
          current-uname        (:current-username db)
          current-pwd          (:current-password db)

          ^js/Gun gun-lib      (or (:gun-lib db) (js/Gun. peer-url))
          gun-app-user         (or (:gun-app-user db))
          gun-browser-user     (or (:gun-browser-user db))


          need-user            (case which-user
                                 :gun-app-user gun-app-user
                                 :gun-browser-user gun-browser-user)


          leave-user           (case which-user
                                 :gun-app-user gun-browser-user
                                 :gun-browser-user gun-app-user)

          ^js/Gun created-user (or need-user (.user gun-lib))
          other-user           (case which-user
                                 :gun-app-user :gun-browser-user
                                 :gun-browser-user :gun-app-user)
          new-db               {:gun-lib   gun-lib
                                which-user created-user
                                other-user leave-user}]


      (when leave-user
        (let [^js/Gun leave-user leave-user]
          (.leave
            leave-user
            #js {}
            (fn [^js/Object d]
              (js/console.log "$$$ left user: " d)))))


      (js/console.log "$$$ need user .is test: "
                      (js/Gun.is created-user))

      (let [[u p] (if (= :gun-browser-user which-user)
                    ["browser" "browserpass"]
                    [current-uname current-pwd])]

        (if signup?

          (let [[signup-u signup-p] signup?]
            (when (and signup-u signup-p)
              (js/console.log "$$$ Creating user: " signup-u signup-p)
              (.create created-user signup-u signup-p
                       (partial got-user-signup created-user signup-u signup-p))))

          (if (and u p)
            (.auth created-user
                   u
                   p
                   (fn [^js/Object d]
                     (js/console.log "$$$ authed user: " d " next evt: " next-evt)
                     (if (.-err d)
                       (do
                         (nav-to "/")
                         (when-not (= "User is already being created or authenticated!" (.-err d))
                           (js/alert (str "Login error for user: " u " : " (.-err d)))))
                       (when next-evt
                         (let [[evt-k evt-params] (split-at 1 next-evt)]
                           (re-frame/dispatch
                             (vec (concat evt-k [created-user gun-lib] evt-params)))))))
                   #js {})
            (nav-to "/"))))

      (js/console.log "$$$ return db: " new-db)
      {:db (merge db new-db)})))





(re-frame/reg-event-fx
  ::gun-got-browser-wall-fact
  (fn [{:keys [db]} [_ doc-key data]]

    (let [data-map   (dissoc (js->clj data) "_")
          old-map    (:gun-wall-facts db)
          tx-id      (last (goog.string/splitLimit doc-key "/" 100))
          timestamps (str (js/Date.
                            (js/parseInt
                              (get-in (js->clj data) ["_" ">" "label"]))))

          d          {:label                 (get data-map "label")
                      :user                  (get data-map "username")
                      :source-txt            (get data-map "secret")
                      :facthash              (get data-map "digest")
                      :proof-hashgraph-tx-id (get data-map "proof-hashgraph-tx-id")
                      :proof-hashgraph-tx-ts (get data-map "proof-hashgraph-tx-ts")
                      :path                  doc-key
                      :tx-id                 tx-id
                      :tx-ts                 timestamps}
          new-map    (vec (set (concat (if
                                         (and (:label d) (:user d) (:facthash d) tx-id)
                                         [d]
                                         [])
                                       old-map)))]
      {:db (assoc db :gun-wall-facts new-map)})))


(defn ^:export got-browser-wall-facts [data key]
  (re-frame/dispatch [::gun-got-browser-wall-fact key data]))


(re-frame/reg-event-fx
  ::gun-get-browser-facts
  (fn [{:keys [db]} [_ ^js/Gun gun-browser-user ^js/Gun gun-lib]]

    (js/console.log "browser user: " gun-browser-user)
    (->
      (.get gun-lib (str "~" server-pk))
      (.get gun-v2-certified-wall-coll)
      (.map)
      (.not (fn [^js/Object d] (js/console.error "bfact wall nf: " d)))
      (.on got-browser-wall-facts))
    {:db (assoc db :gun-wall-facts [])}))


