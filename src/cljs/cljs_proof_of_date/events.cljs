(ns cljs-proof-of-date.events
  (:require
    [re-frame.core :as re-frame]
    [cljs-proof-of-date.db :as db]
    [cljs-proof-of-date.digest :as digest]
    [goog.crypt :as crypt]
    [ajax.core :as ajax]
    [day8.re-frame.tracing :refer-macros [fn-traced]]
    )
  (:import goog.crypt.Sha256))


(def peer-url "https://proofof.date:8765/gun")


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
  (js/console.log "User ;eave result: " d))


(re-frame/reg-event-fx
  ::home-page-user-password-logout
  (fn [{:keys [db]} [_ username]]
    (let [^js/Gun gun-user (:gun-user db)]
      (when gun-user
        (js/console.log "logged out: " username "  /  "
                        (.leave gun-user
                                #js {}
                                got-user-leave))))
    (nav-to "#/")
    {:db (dissoc db :user)}))


(defn ^:export got-user-auth [username ^js/Object d]
  (js/console.log "User auth res: " d " error: " (.-err d))
  (if (.-err d)
    (do (js/console.error "Incorrect user" (.-err d) username)
        (re-frame/dispatch [::home-page-user-password-login-failure username (.-err d)]))
    (do
      (re-frame/dispatch [::home-page-user-password-login-success username (str "#/home/" username)]))))



(re-frame/reg-event-fx
  ::home-page-user-password-login
  (fn [{:keys [db]} [_ username password post-init?]]
    (let [
          ^js/Gun gun-user (:gun-user db)
          ]
      (try
        (if-not gun-user
          (do

            (println "User login: need to init gun! ")
            (re-frame/dispatch [::init-gun])
            (re-frame/dispatch [::home-page-user-password-login username password false]))
          (do
            (println "User left prior to login, maybe leave: "
                     (when-not post-init?
                       (.leave gun-user
                               #js {}
                               got-user-leave)))
            (js/console.log "gun bfore auth: " gun-user)
            (.auth gun-user username password
                   (partial got-user-auth username)
                   #js {})))
        (catch ExceptionInfo e
          (js/console.error "Error handling login: " (js/JSON.stringify e))))

      {:db db})))



(re-frame/reg-event-fx
  ::home-page-user-password-login-success
  (fn [{:keys [db]} [_ username loc]]
    (let []
      (when loc
        (js/console.log "Login success, nav to : " loc)
        (nav-to loc))
      {:db (assoc (dissoc db :login-message)
             :user username)})))


(re-frame/reg-event-fx
  ::home-page-user-password-login-failure
  (fn [{:keys [db]} [_ username reason]]
    (let []
      {:db (assoc db :login-message (str "Failed to log in " username " , Reason: " reason))})))


(re-frame/reg-event-fx
  ::home-page-user-password-signup-failure
  (fn [{:keys [db]} [_ username reason]]
    (let []

      {:db (assoc db :login-message (str "Failed to sign up " username " , Reason: " reason))})))


(defn ^:export got-user-signup [gun-user username password ^js/Object e]
  (if (.-err e)
    (do
      (js/console.error "Create user error " (.-err e))
      (re-frame/dispatch [::home-page-user-password-signup-failure username (.-err e)]))
    (do
      (js/console.log "Created user! " e " , gun: " gun-user)
      (re-frame/dispatch [::home-page-user-password-login username password false])
      false)))


(re-frame/reg-event-fx
  ::home-page-user-password-signup
  (fn [{:keys [db]} [_ username password]]

    (let [^js/Gun gun-user (:gun-user db)]
      (if-not gun-user
        (do
          (js/console.log "Create user init " username)
          (re-frame/dispatch [::init-gun])
          (re-frame/dispatch [::home-page-user-password-signup username password]))
        (try
          (.create gun-user username password
                   (partial got-user-signup gun-user username password))
          (catch ExceptionInfo e
            (js/console.error "Error creating GUN user: " (js/JSON.stringify e)))))

      {:db (dissoc db :login-message)})))


(defn ^:export got-user-fact [username data key]
  (re-frame/dispatch [::gun-got-user-fact username key data]))


(re-frame/reg-event-fx
  ::gun-get-user-facts
  (fn [{:keys [db]} [_ username]]
    (let [^js/Gun gun-user (:gun-user db)
          ^js/Gun gun      (:gun db)]
      (if gun-user
        (do (-> (.get gun-user "user-facts")
                (.get username)
                (.map)
                (.on
                  (partial got-user-fact username))))
        (do
          (js/console.log "user not logged in")
          (re-frame/dispatch [::init-gun])
          (re-frame/dispatch [::gun-get-user-facts username]))))
    {:db (assoc db :user username
                   :gun-user-facts [])
     :fx [[:dispatch [::gun-get-all-facts false]]]}))


(re-frame/reg-event-fx
  ::gun-got-user-fact
  (fn [{:keys [db]} [_ username key data]]
    (let [
          data-map   (js->clj data)
          timestamps (str (js/Date.
                            (js/parseInt
                              (get-in data-map ["_" ">" "label"]))))
          path       (str (get-in data-map ["_" "#"]))
          old-map    (:gun-user-facts db)
          d          {:recv-acct-id "recv-acct-id"
                      :send-acct-id "send-acct-id"
                      :label        (get data-map "label")
                      :user         username
                      :source-txt   (get data-map "secret")
                      :facthash    (get data-map "digest")
                      :path         path
                      :tx-id        key
                      :tx-ts        timestamps}
          new-map    (vec (set (concat [d] old-map)))]
      {:db (assoc db :gun-user-facts new-map)})))


(defn ^:export get-user-facts [user ]
  (re-frame/dispatch [::gun-get-user-facts user]) )

(defn- get-wall-fact-from-db [db-facts match-user
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




    ;; TODO: this doesnt work as intended fully because we need to remove the fact ID from the list, not set the id's contents to nil
    ;; - it is a hack to set it to null because the values are still there, so we are burning precious localstorage space
    ;; - it seems to delete non-user fact, but the user facts are still there but empty data




    (let [
          ^js/Gun gun      (:gun db)
          ^js/Gun gun-user (:gun-user db)
          {:keys [source-txt label facthash user tx-id]} fact
          wall-data        (get-wall-fact-from-db (:gun-wall-facts db) user
                                                  facthash
                                                  label)
          _                (-> (.get gun-user "user-facts")
                               (.get user)
                               (.get tx-id)
                               (.put nil (partial get-user-facts user)))

          _                (when wall-data
                             (-> (.get gun "user-facts")
                                 (.get user)
                                 (.get (:tx-id wall-data))
                                 (.put  nil)))]

      {:db db
       ;:fx [[:dispatch [::gun-get-user-facts user]]]
       })))




(re-frame/reg-event-fx
  ::gun-hide-fact
  (fn [{:keys [db]} [_ data]]
    (let [^js/Gun gun      (:gun db)
          ^js/Gun gun-user (:gun-user db)

          {:keys [source-txt label facthash user tx-id]} data
          _                (-> (.get gun-user "user-facts")
                               (.get user)
                               (.get tx-id)
                               (.put #js {:label    label
                                          :digest   facthash
                                          :secret   nil
                                          :username user}))
          del-label        label
          del-hash         facthash
          del-user         user
          wall-data        (get-wall-fact-from-db (:gun-wall-facts db) del-user
                                                  del-hash
                                                  del-label)

          _                (when wall-data
                             (-> (.get gun "user-facts")
                                 (.get user)
                                 (.get (:tx-id wall-data))
                                 (.put #js {:label    label
                                            :digest   facthash
                                            :secret   nil
                                            :username user})))]

      (js/console.info "hide user fact txt: " data
                       " , wall fact: " wall-data)
      {:db db
       :fx [[:dispatch [::gun-get-user-facts user]]]})))



(re-frame/reg-event-fx
  ::home-page-fact-cancel-unlock-btn-press
  (fn [{:keys [db]} _]
    {:db (assoc db :home-page-fact-unlock-editing false)}))





(re-frame/reg-event-fx
  ::home-page-cancel-fact-btn-press
  (fn [{:keys [db]} _]
    {:db (assoc db :home-page-add-fact-editing false)}))


(re-frame/reg-event-fx
  ::home-page-fact-save-unlock-btn-press
  (fn [{:keys [db]} [_ puser plabel ptx-id pcandidate-txt pfacthash]]

    (js/console.warn
      "Try unlock. u = " puser
      " , l = " plabel
      " , txid = " ptx-id
      " , txt = " pcandidate-txt
      " , h = " pfacthash)

    (let [^js/Gun gun      (:gun db)
          ^js/Gun gun-user (:gun-user db)
          phash            (digest/get-sha256-str pcandidate-txt)
          correct?         (= phash pfacthash)]

      (if correct?
        (do

          (-> (.get gun-user "user-facts")
              (.get puser)
              (.get ptx-id)
              (.put #js {:label    plabel
                         :digest   pfacthash
                         :secret   pcandidate-txt
                         :username puser}))
          (let [wall-data (->> (:gun-wall-facts db)
                               (filter (fn [{:keys [user facthash label]}]
                                         (and (= user puser)
                                              (= facthash pfacthash)
                                              (= label plabel))))
                               first)

                _         (when wall-data
                            (-> (.get gun "user-facts")
                                (.get puser)
                                (.get (:tx-id wall-data))
                                (.put #js {:label    plabel
                                           :digest   pfacthash
                                           :secret   pcandidate-txt
                                           :username puser})))]))
        (js/console.log "Incorrect src text: " pcandidate-txt " -> " phash " != " pfacthash))
      {:db (assoc db :home-page-fact-unlock-editing (not correct?))})))




(re-frame/reg-event-fx
  ::home-page-save-fact-btn-press
  (fn [{:keys [db]} [_ user label secret]]
    (let [^js/Gun gun      (:gun db)
          ^js/Gun gun-user (:gun-user db)
          digest           (digest/get-sha256-str secret)

          _                (js/console.log "JSON fact has digest: " digest)

          _                (-> (.get gun-user "user-facts")
                               (.get user)
                               (.set #js {:label    label
                                          :secret   secret
                                          :digest   digest
                                          :username user}))
          _                (-> (.get gun "user-facts")
                               (.get user)
                               (.set #js {:label    label
                                          :secret   secret
                                          :digest   digest
                                          :username user}))]
      {:db (assoc db :home-page-add-fact-editing false
                     )})))


(re-frame/reg-event-fx
  ::home-page-set-user
  (fn [{:keys [db]} [_ username]]
    {:db (assoc db :user username)}))


(re-frame/reg-event-fx
  ::gun-got-fact
  (fn [{:keys [db]} [_ key-path fact tx-id tx-ts]]
    (let [data-map (js->clj fact)
          d        {:label      (get data-map "label")
                    :user       (get data-map "username")
                    :source-txt (get data-map "secret")
                    :facthash  (get data-map "digest")
                    :path       key-path
                    :tx-id      tx-id
                    :tx-ts      tx-ts}]
      (js/console.log "got fact : " d)
      {:db (assoc db :gun-fact d)})))



(defn ^:export got-single-fact [key-path fact]
  (let [timestamps (str (js/Date.
                          (js/parseInt
                            (get-in (js->clj fact) ["_" ">" "label"]))))]
    (re-frame/dispatch [::gun-got-fact
                        key-path
                        fact
                        (last (goog.string/splitLimit key-path "/" 100))
                        timestamps])))


(re-frame/reg-event-fx
  ::gun-get-fact
  (fn [{:keys [db]} [_ key-id redir?]]
    (let [key-path (goog.string/replaceAll key-id "|" "/")
          gun      (:gun db)
          redir    (if redir? (str "#/fact/" key-id)
                              false)]

      (js/console.log "get fact for path: " key-path " , redir? " redir? " , redir=" redir)

      (if-not gun
        (do (re-frame/dispatch [::init-gun redir])
            (re-frame/dispatch [::gun-get-fact key-id redir?]))
        (do
          (js/console.log "authorized, making rpoof req...")
          (-> (.get gun key-path)
              (.once
                (partial got-single-fact key-path)))))

      {:db db})))


(defn ^:export got-wall-fact [doc-key data2]
  (let [timestamps (str (js/Date.
                          (js/parseInt
                            (get-in (js->clj data2) ["_" ">" "label"]))))]
    (re-frame/dispatch [::gun-got-wall-fact
                        doc-key
                        data2
                        (last (goog.string/splitLimit doc-key "/" 100))
                        timestamps])))



(defn ^:export got-wall-facts [gun data key]
  (let [clj-data (js->clj data)
        doc-keys (->> (dissoc clj-data "_")
                      vals
                      (map #(do
                              (get % "#"))))]
    (doseq [doc-key doc-keys]
      (-> (.get gun doc-key)
          (.once
            (partial got-wall-fact doc-key))))))


(re-frame/reg-event-fx
  ::gun-get-all-facts
  (fn [{:keys [db]} [_ redir?]]
    (let [^js/Gun gun-user (:gun-user db)
          ^js/Gun gun      (:gun db)]
      (if gun-user
        (do (-> (.get gun "user-facts")
                (.map)
                (.on
                  (partial got-wall-facts gun))))
        (do
          (re-frame/dispatch [::init-gun (when redir? "#/wall")])
          (re-frame/dispatch [::gun-get-all-facts redir?])))
      {:db (assoc db :gun-wall-facts [])})))


(re-frame/reg-event-fx
  ::gun-got-wall-fact
  (fn [{:keys [db]} [_ doc-key data tx-id tx-tss]]

    (let [data-map (js->clj data)
          old-map  (:gun-wall-facts db)
          d        {:label      (get data-map "label")
                    :user       (get data-map "username")
                    :source-txt (get data-map "secret")
                    :facthash  (get data-map "digest")
                    :path       doc-key
                    :tx-id      tx-id
                    :tx-ts      tx-tss}
          new-map  (vec (set (concat (if
                                       (and (:label d) (:user d) (:facthash d) tx-id)
                                       [d]
                                       [])
                                     old-map)))]
      {:db (assoc db :gun-wall-facts new-map)})))




(re-frame/reg-event-db
  ::clear-user-data
  (fn [db _]
    (dissoc db :user-info :user)))



(defn map-fact [result]
  (let [uname        (get result :username)
        recv-acct-id (get result :receiver_account_id)
        send-acct-id (get result :sender_account_id)
        pname        (get result :label)
        phash        (get result :hash)
        tx-id        (get result :tx_id)
        tx-ts        (get result :tx_ts)
        source-txt   (get result :source_txt)
        d            {:recv-acct-id recv-acct-id
                      :send-acct-id send-acct-id
                      :label        pname
                      :user         uname
                      :source-txt   source-txt
                      :facthash    phash
                      :tx-id        tx-id
                      :tx-ts        tx-ts}]
    d))




(defn ^:export got-username [redirect fetched-username]
  (let [redir (or redirect
                  (str "#/home/" fetched-username))]
    (re-frame/dispatch [::home-page-user-password-login-success
                        fetched-username
                        redir])
    (js/console.log "user name: " fetched-username " w redir: " redir)))

(re-frame/reg-event-fx
  ::init-gun
  (fn [{:keys [db]} [_ redirect]]
    (if (:gun db)
      {:db db}
      (do
        (js/console.log "init gun, redir: " redirect)
        (try (let [
                   gun      (js/Gun. peer-url)
                   ;gun      (js/Gun. )
                   gun-user (-> gun
                                (.user)
                                (.recall
                                  #js {:sessionStorage true}))]

               (when (js/Gun.is gun-user)
                 (-> (.get gun-user "alias")
                     (.once
                       (partial got-username redirect))))

               ;(when redirect (nav-to redirect))

               {:db (assoc db :gun gun
                              :gun-user gun-user)})
             (catch ExceptionInfo e
               (js/console.error "Error init GUN: " (js/JSON.stringify e))))))))


