(ns cljs-proof-of-date.events
  (:require
    [re-frame.core :as re-frame]
    [cljs-proof-of-date.db :as db]
    [cljs-proof-of-date.lib.gun :as gunlib]
    [cljs-proof-of-date.lib.digest :as digest]
    [goog.crypt :as crypt]
    [ajax.core :as ajax]
    [day8.re-frame.tracing :refer-macros [fn-traced]]
    [clojure.string :as string]
    [cljs-proof-of-date.config :as config])
  (:import goog.crypt.Sha256))


(defn nav-to [nav-hash]
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
  ::set-active-fact
  (fn-traced [db [_ active-fact-id]]
    (assoc db :active-fact-id active-fact-id)))

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


(defn ^:export got-user-signup [which-user username password ^js/Object e]
  (let [^js/Gun gun-user (gunlib/get-user which-user)
        ^js/Gun gun-lib  (gunlib/get-user ::gunlib/public)]
    (if (.-err e)
      (do
        (js/console.error "Create user error " (.-err e))
        (js/alert (str "Create error: " (.-err e)))

        ;; TODO if (.-err e) is user already created, then call auth

        (re-frame/dispatch [::home-page-user-password-signup-failure username (.-err e)]))
      (do
        (js/console.log "Created user! " e " , gun: " gun-user)
        (re-frame/dispatch [::home-page-user-password-login username password])

        (let [gun-app-user-kpair (gunlib/get-user-pair gun-user)]

          (gunlib/set-path {:user     gun-lib
                            :path     [gunlib/gun-pub-user-meta-coll username]
                            :data     {:created (.getTime (js/Date.))
                                       :pub-key (gunlib/get-user-pub gun-app-user-kpair)}
                            :callback #(js/console.log
                                         "V2 !!!!! Set user meta "
                                         %)}))

        (nav-to (str "#/user/" username))
        false))))


(re-frame/reg-event-fx
  ::home-page-user-password-signup
  (fn [{:keys [db]} [_ username password]]
    {:db (dissoc db :login-message
                 :current-username username
                 :current-password password)
     :fx [[:dispatch [::init-gun-and-users
                      nil [username password] ::gunlib/app-user [::gun-get-user-facts username]]]]}))


(defn ^:export got-user-fact [username data key]
  (re-frame/dispatch [::gun-got-user-fact username key data]))



(re-frame/reg-event-fx
  ::gun-get-user-facts
  (fn [{:keys [db]} [_ ^js/Gun gun-app-user ^js/Gun gun-lib username]]
    (let [^js/Gun gun-app-user (or gun-app-user (gunlib/get-user ::gunlib/app-user))]
      (gunlib/map-path-on
        {:path     [gunlib/gun-prv-user-coll]
         :user     gun-app-user
         :callback (partial got-user-fact username)}))
    {:db (assoc db :user username)}))


(re-frame/reg-event-fx
  ::gun-got-user-fact
  (fn [{:keys [db]} [_ username data-key data]]
    (if-not data
      {:db (assoc db :gun-user-facts (remove #(= data-key (:tx-id %)) (:gun-user-facts db)))}
      (let [data-map   (js->clj data)
            timestamps (str (js/Date.
                              (js/parseInt
                                (str (get-in data-map ["_" ">" "label"])))))
            path       (str (get-in data-map ["_" "#"]))
            old-map    (:gun-user-facts db)
            d          {:label      (get data-map "label")
                        :user       username
                        :source-txt (get data-map "secret")
                        :facthash   (get data-map "digest")
                        :path       path
                        :tx-id      data-key
                        :tx-ts      timestamps}
            wall-fact  (first
                         (filter
                           (fn [f]
                             (and (= (:label d) (:label f))
                                  (= (:user d) (:user f))
                                  (= (:source-txt d) (:source-txt f))
                                  (= (:facthash d) (:facthash f))))
                           (:gun-wall-facts db)))
            d          (merge
                         d
                         (when (and wall-fact
                                    (:proof-hashgraph-tx-ts wall-fact))
                           {:proof-hashgraph-tx-id (:proof-hashgraph-tx-id wall-fact)
                            :proof-hashgraph-tx-ts (js/parseInt
                                                     (:proof-hashgraph-tx-ts wall-fact))}))
            new-map    (vec
                         (reverse
                           (sort-by
                             :proof-hashgraph-tx-ts
                             (set (concat
                                    [d]
                                    (remove #(= path (:path %)) old-map))))))]
        {:db (assoc db :gun-user-facts new-map)}))))


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

(defn- send-delete-secret-payload [^js/Gun gun-lib app-user-epub app-user-pub wall-data secret]

  (js/console.log "Got client secret: " secret " for " app-user-epub)

  (gunlib/encrypt {:data     app-user-pub
                   :secret   secret
                   :callback (fn [enc-data]

                               (js/console.log "%% enc data: " enc-data)

                               (gunlib/put-path
                                 {:path     [gunlib/gun-pub-user-coll (:path wall-data)]
                                  :user     gun-lib
                                  :data     {:delete         true
                                             :pub-key        app-user-pub
                                             :epub-key       app-user-epub
                                             :pub-key-secret enc-data}
                                  :callback #()}))}))

(defn- send-delete-payload
  [^js/Gun gun-lib ^js/Gun gun-app-user-kpair wall-data
   ^js/Object server-user-data server-user-key]

  (js/console.log "Got server pub data: " server-user-data server-user-key)

  (let [^js/String server-user-epub (gunlib/get-user-epub server-user-data)
        ^js/String app-user-epub    (gunlib/get-user-epub gun-app-user-kpair)
        ^js/String app-user-pub     (gunlib/get-user-pub gun-app-user-kpair)]
    (gunlib/secret
      {:epub     server-user-epub
       :pair     gun-app-user-kpair
       :callback (partial send-delete-secret-payload
                          gun-lib app-user-epub app-user-pub wall-data)})))

(re-frame/reg-event-fx
  ::gun-delete-fact
  (fn [{:keys [db]} [_ fact]]

    (js/console.log "Delete fact: " fact)

    (let [^js/Gun gun-lib      (gunlib/get-user ::gunlib/public)
          ^js/Gun gun-app-user (gunlib/get-user ::gunlib/app-user)

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
                                 (last splits))

          _                    (js/console.log "%%% delete user: " path " splits: " splits)

          gun-app-user-kpair   (gunlib/get-user-pair gun-app-user)

          _                    (gunlib/put-path
                                 {:path     [gunlib/gun-prv-user-coll path]
                                  :user     gun-app-user
                                  :data     nil
                                  :callback (partial get-user-facts user)})

          _                    (when wall-data
                                 (js/console.log "%% delete wall path :" (:path wall-data) gun-lib)

                                 (gunlib/get-path-once
                                   {:path     [(str "~" gunlib/server-pubkey)]
                                    :user     gun-lib
                                    :callback (partial send-delete-payload
                                                       gun-lib gun-app-user-kpair wall-data)}))]

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
    (let [^js/Gun gun-lib            (gunlib/get-user ::gunlib/public)
          ^js/Gun gun-app-user       (gunlib/get-user ::gunlib/app-user)
          digest                     (digest/get-sha256-str secret)
          ^js/SEA gun-app-user-kpair (gunlib/get-user-pair gun-app-user)]

      (js/console.log
        "Saved fact has digest data: "
        user " : " label " : " digest)

      (gunlib/sign
        {:data     digest
         :pair     gun-app-user-kpair
         :callback (fn [^js/Object signed]

                     (js/console.log "GOT SIGNED : " signed)

                     (gunlib/set-path {:user     gun-app-user
                                       :path     [gunlib/gun-prv-user-coll]
                                       :data     {:label     label
                                                  :secret    secret
                                                  :digest    digest
                                                  :username  user
                                                  :pub-key   (gunlib/get-user-pub gun-app-user-kpair)
                                                  :signature signed}
                                       :callback #(js/console.log
                                                    "V2 !!!!! Got result of set user fact!  "
                                                    %)})

                     (gunlib/set-path {:user     gun-lib
                                       :path     [gunlib/gun-pub-user-coll]
                                       :data     {:label     label
                                                  :secret    secret
                                                  :digest    digest
                                                  :username  user
                                                  :pub-key   (gunlib/get-user-pub gun-app-user-kpair)
                                                  :signature signed}
                                       :callback #(js/console.log
                                                    "V2 !!!!! Got result of set public fact!  "
                                                    %)}))})

      {:db (assoc db :home-page-add-fact-editing false)})))



(re-frame/reg-event-fx
  ::home-page-user-password-login
  (fn [{:keys [db]} [_ username password]]
    {:db (assoc db
           :current-username username
           :current-password password)}))



(re-frame/reg-event-fx
  ::init-gun-and-users
  (fn [{:keys [db]} [_ process-fact-key-id signup? which-user next-evt]]
    (js/console.log "init user: " which-user)
    (let [current-uname (:current-username db)
          current-pwd   (:current-password db)

          [user-name user-password] (if (= ::gunlib/browser-user which-user)
                                      ["browser" "browserpass"]
                                      [current-uname current-pwd])
          leave-user   (case which-user
                          ::gunlib/app-user ::gunlib/browser-user
                          ::gunlib/browser-user ::gunlib/app-user)]

      (gunlib/logout leave-user {:callback (fn [^js/Object d]
                                              (js/console.log "V2 left user: " d))})


      (if signup?
        (let [[signup-u signup-p] signup?]
          (when (and signup-u signup-p)
            (js/console.log "V2 Creating user: " signup-u signup-p)
            (gunlib/signup which-user {:username signup-u
                                       :password signup-p
                                       :callback (partial got-user-signup which-user signup-u signup-p)})))
        (if (and user-name user-password)

          (gunlib/login
            which-user
            {:username user-name
             :password user-password
             :callback (fn [^js/Object d]
                         (if (.-err d)
                           (do
                             (nav-to "/")
                             (when-not (= "User is already being created or authenticated!" (.-err d))
                               (js/alert (str "V2 Login error for user: " user-name " : " (.-err d)))))
                           (when next-evt
                             (let [[evt-k evt-params] (split-at 1 next-evt)]
                               (re-frame/dispatch
                                 (vec (concat evt-k
                                              [(gunlib/get-user which-user) (gunlib/get-user ::gunlib/public)]
                                              evt-params)))
                               (when process-fact-key-id
                                 (re-frame/dispatch [::set-active-fact process-fact-key-id]))))))})
          (nav-to "/")))
      {:db (merge db {:gun-user-facts []})})))


(re-frame/reg-event-fx
  ::gun-got-browser-wall-fact
  (fn [{:keys [db]} [_ doc-key data]]
    (if-not data
      {:db (assoc db :gun-wall-facts (remove #(= doc-key (:tx-id %)) (:gun-wall-facts db)))}
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
                        :signature             (get data-map "signature")
                        :proof-hashgraph-tx-ts (when (get data-map "proof-hashgraph-tx-ts")
                                                 (js/parseInt (get data-map "proof-hashgraph-tx-ts")))
                        :path                  doc-key
                        :tx-id                 tx-id
                        :tx-ts                 timestamps}
            new-map    (vec
                         (reverse
                           (sort-by
                             :proof-hashgraph-tx-ts
                             (set (concat
                                    (if
                                      (and (:label d) (:user d) (:facthash d) tx-id)
                                      [d]
                                      [])
                                    old-map)))))]
        (merge {:db (assoc db :gun-wall-facts new-map)}
               (when (:current-username db)
                 {:fx [[:dispatch
                        [::gun-get-user-facts
                         (gunlib/get-user ::gunlib/browser-user)
                         (gunlib/get-user ::gunlib/public)
                         (:current-username db)]]]}))))))


(re-frame/reg-event-fx
  ::gun-get-browser-facts
  (fn [{:keys [db]} [_ ^js/Gun gun-browser-user ^js/Gun gun-lib]]
    (gunlib/map-path-on
      {:path     [(str "~" gunlib/server-pubkey) gunlib/gun-certified-wall-coll]
       :user     gun-lib
       :callback (fn [data key] (re-frame/dispatch [::gun-got-browser-wall-fact key data]))})
    {:db db}))


(re-frame/reg-event-fx
  ::fact-search
  (fn [{:keys [db]} [_ txt]]

    (js/console.log "Search: " txt)
    (let [old-map (or (:gun-wall-facts-orig db) (:gun-wall-facts db))
          new-map (filter
                    (fn [fact]
                      (or (string/includes? (:source-txt fact) txt)
                          (string/includes? (:label fact) txt)
                          (string/includes? (:user fact) txt)))
                    old-map)]
      {:db (if (or (nil? txt)
                   (= "" txt))
             (dissoc (assoc db :gun-wall-facts old-map)
                     :gun-wall-facts-orig
                     :search-txt)
             (assoc db :gun-wall-facts new-map
                       :gun-wall-facts-orig old-map
                       :search-txt txt))})))

(comment)


;; TODO user pools
;; - user create pool
;;   - write to public coll      /pools/id/{pubkey , users: Set({ userId, userPubKey, poolKeySignedByUser })}
;;   - write to privat coll /user-pools/id/{ poolPair }
;; - user invite user to pool
;;   - write to public coll /pool-invites/username/invite-id/{ poolIdEncrypted: SEA.enc(poolId, SEA.secret(user.epub, poolPair))
;;                                                             poolPrivKeyEnc: ^ but poolPair private ,
;;                                                             signature }
;;      -> therefore the invited user can decrypt what the poolId and private key is,
;;      - save to priv /user-pools/id/{ poolPair }
;;   - write: set /pools/id/users/{ userId, userPubKey, poolKeySignedByUser } for new user that accepted invite
;; - user create facts




;; TODO
;; - user pools with either quorum or date unlock of :source-text for everyones facts
;;   - user create pool:
;;     - user inputs other usernames
;;     - creates a pair for the pool
;;     - set pub pools {pub: poolpubkey, users: set({pubkey, username, poolPubKeySignedByUser}), facts: set() , unlock_strategy: {type:countdown , date: "34r423"} }
;;     - set priv user-pools with same ID as pub, pub user-pools/id { facts: set() , poolPair } this coll has input-txt
;;     - create invites in a pub coll pool-invites/username/{poolId, poolPubKey, poolprivkeyenc: SEA.enc(poolPrivKey, SEA.secret(user.epub, ppoolPair)) }
;;     - invited user sees invite, unlocks priv key, asves it, adds itself to users
;;     - in user route, separete table per pool. on fact add, specifict which pool to add to, if any (or public)
