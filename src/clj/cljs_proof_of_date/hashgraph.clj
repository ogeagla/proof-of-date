(ns cljs-proof-of-date.hashgraph
  (:require [ring.util.response :refer [response content-type resource-response]]
            )
  (:import (com.hedera.hashgraph.sdk
             PrivateKey Client AccountId Hbar AccountDeleteTransaction
             TransactionResponse TransactionReceipt Status AccountCreateTransaction
             AccountInfoQuery AccountUpdateTransaction TransactionRecordQuery
             TransferTransaction AccountBalanceQuery TransactionRecord
             AccountBalance FileCreateTransaction FileId FileContentsQuery
             FileAppendTransaction Key)
           (io.github.cdimascio.dotenv Dotenv)
           (java.security MessageDigest)
           (java.time Instant)
           (com.google.protobuf ByteString)
           (java.util Date UUID)))


;; gigabar 1 Gℏ = 1,000,000,000 ℏ
;; megabar 1 Mℏ = 1,000,000 ℏ
;; kilobar 1 kℏ = 1,000 ℏ
;; hbar 1 ℏ = 1 ℏ
;; millibar 1,000 mℏ = 1 ℏ
;; microbar 1,000,000 μℏ = 1 ℏ
;; tinybar 100,000,000 tℏ = 1 ℏ


;(context "/username" []
;  (GET "/:username/balances" [username] (hg/get-user-balance-info username))
;  (GET "/:username" [username] (hg/get-user-for-username username))
;  (GET "/:username/proof" [username] (hg/get-proofs-for-username username))
;  (GET "/:username/deleted_proof" [username] (hg/get-deleted-proofs-for-username username))
;  )
;
;(context "/sha256" []
;  (POST "/" [] hg/post-sha256))
;
;(context "/proof_undelete" []
;  (POST "/" [] hg/undelete-proof))
;
;
;(context "/proof" []
;  (POST "/" [] hg/post-proof)
;  (POST "/:tx-id/unlock" [tx-id] (hg/post-proof-unlock tx-id))
;  (GET "/" [] hg/get-proofs)
;  (DELETE "/" [] hg/delete-proof))


(def ^AccountId testnet-admin-acct-id
  (AccountId/fromString
    (-> (Dotenv/load) (.get "MY_ACCOUNT_ID"))))

(def ^PrivateKey testnet-admin-priv-key
  (PrivateKey/fromString
    (-> (Dotenv/load) (.get "MY_PRIVATE_KEY"))))


(defn- client-for-net [net]
  (case net
    :test (Client/forTestnet)
    :main (Client/forMainnet)))

(defn client-for-user-w-max-pay [net ^AccountId acct-id max-pay]
  (->
    (client-for-net net)
    (.setOperator acct-id testnet-admin-priv-key)
    (.setMaxQueryPayment max-pay)))

(defn client-for-user [net ^AccountId acct-id]
  (client-for-user-w-max-pay net acct-id (Hbar/fromTinybars 500000)))


(def ^Client testnet-admin-client
  (client-for-user :test testnet-admin-acct-id))



(defn create-file [str-data priv-key client]

  ;; TODO all accounts, files, and contracts expire in 3 months? accounts and contracts auto-renew and charge the wallets to do so
  ;; - hedera disabled this

  (try
    (let [expire-milli                 (+ 10000000000 (.getTime (Date.)))
          ^FileCreateTransaction tx    (-> (FileCreateTransaction.)
                                           ;(.setExpirationTime (Instant/ofEpochMilli expire-milli))
                                           (.setKeys (into-array Key [priv-key]))
                                           (.setMaxTransactionFee (Hbar/fromTinybars 200000000))
                                           (.setContents str-data))

          ;; //Prepare transaction for signing, sign with the key on the file, sign with the client operator key and submit to a Hedera network
          ;TransactionResponse txResponse = modifyMaxTransactionFee.freezeWith(client).sign(fileKey).execute(client);
          ^TransactionResponse tx-resp (-> tx
                                           (.freezeWith client)
                                           (.sign priv-key)
                                           (.execute client))
          ^TransactionReceipt receipt  (.getReceipt tx-resp client)
          ^FileId new-file-id          (.-fileId receipt)]
      (println "new file tx resp: " tx-resp " file ID : " (.toString new-file-id))
      new-file-id)
    (catch Exception e
      (println "Error creating file " e)
      nil)))


(defn append-to-file [file-id str-data priv-key client]
  (let [^TransactionResponse tx-resp (-> (FileAppendTransaction.)
                                         (.setFileId file-id)
                                         (.setMaxTransactionFee (Hbar/fromTinybars 200000000))
                                         (.setContents str-data)
                                         (.freezeWith client)
                                         (.sign priv-key)
                                         (.execute client))
        ^TransactionReceipt receipt  (.getReceipt tx-resp client)
        ^Status status               (.-status receipt)]
    (println "File append status: " (.toString status))
    status))


(defn get-file [file-id client]
  (let [^FileContentsQuery query (-> (FileContentsQuery.)
                                     (.setMaxQueryPayment (Hbar/fromTinybars 25))
                                     (.setFileId file-id))
        ^ByteString contents     (-> query
                                     (.execute client))
        ^String data-utf8        (-> contents .toStringUtf8)]
    data-utf8))





(defn file-demo [priv-key client]
  (let [file-id        (create-file "hello world 123456 )(*!" priv-key client)
        ;^FileId file-id      (FileId/fromString "0.0.358367")
        file-contents  (get-file file-id client)
        updated1       (append-to-file file-id (str "\nabcdefg 0987654321 " (Date.)) priv-key client)
        file-contents2 (get-file file-id client)]
    (println "got saved file content: " file-contents)
    (println "file udpate: " updated1)
    (println "got saved file content after update: " file-contents2)))



;; /Files


(defn transfer-w-max-pay [user-id ^AccountId acct-id recip-id amt memo client max-pay]
  (try
    (let [^TransferTransaction tx     (-> (TransferTransaction.)
                                          (.addHbarTransfer acct-id (.negated amt))
                                          (.addHbarTransfer recip-id amt)
                                          (.setMaxTransactionFee max-pay)
                                          ;; memo size max is 100 chars
                                          (.setTransactionMemo memo))

          ;_                           (println "Exec tx: " tx)

          ^TransactionResponse resp   (.execute tx client)

          _                           (println "Tx resp: " resp)

          ^TransactionRecord tx-rec   (.getRecord resp client)
          ^TransactionReceipt receipt (.getReceipt resp client)
          ^Status status              (.-status receipt)]
      (println "Tx transfer status: " (.toString status))

      {:tx-resp resp
       :tx-rec  tx-rec})
    (catch Exception e
      (println "Error in transfer: " e))))

(defn transfer [user-id acct-id recip-id amt memo client]
  (transfer-w-max-pay user-id acct-id recip-id amt memo client (Hbar/fromTinybars 100000)))




(defn get-account-balance [^AccountId acct-id client]
  (let [^AccountBalance b (-> (AccountBalanceQuery.)
                              (.setMaxQueryPayment (Hbar/fromTinybars 20))
                              (.setAccountId acct-id)
                              (.execute client))]
    b))




(defn- get-account-info [acct-id client]
  (-> (AccountInfoQuery.)
      (.setAccountId acct-id)
      (.setMaxQueryPayment (Hbar/fromTinybars 25))
      (.execute client)))


(defn update-account [acct-id client]
  (-> (AccountUpdateTransaction.)
      (.setMaxTransactionFee (Hbar/fromTinybars 10))
      (.setSendRecordThreshold (Hbar/fromTinybars 2))
      (.setReceiveRecordThreshold (Hbar/fromTinybars 2))
      (.setAccountId acct-id)
      (.execute client)))


(defn get-tx-record [tx-id client]
  (-> (TransactionRecordQuery.)
      (.setTransactionId tx-id)
      (.setMaxQueryPayment (Hbar/fromTinybars 7))
      (.execute client)))



(defn- transfer-tx [user-id acct-id recip-id amt memo]
  (let [
        client           (client-for-user :test acct-id)
        sender-bal-bef   (-> (get-account-balance acct-id client)
                             .-hbars
                             .getValue)
        recip-bal-bef    (-> (get-account-balance recip-id client)
                             .-hbars
                             .getValue)
        _                (println "balances before tx: " sender-bal-bef " , " recip-bal-bef)
        {:keys [^TransactionResponse tx-resp
                ^TransactionRecord tx-rec]} (transfer user-id acct-id recip-id amt memo client)
        _                (println "tx complete for memo: " (-> tx-rec .-transactionMemo))
        ^Instant tx-ts   (.-consensusTimestamp tx-rec)
        sender-bal-after (-> (get-account-balance acct-id client)
                             .-hbars
                             .getValue)
        recip-bal-after  (-> (get-account-balance recip-id client)
                             .-hbars
                             .getValue)
        resp             {:tx-id (.toString (.-transactionId tx-resp))
                          :tx-ts tx-ts}
        ]


    (println "balances after tx: " sender-bal-after " , " recip-bal-after)
    resp))




(defn create-acct [user-id pub-key ^Client client balTiny]

  (let [^AccountCreateTransaction tx (-> (AccountCreateTransaction.)
                                         (.setKey pub-key)
                                         (.setInitialBalance (Hbar/fromTinybars balTiny)))
        ^TransactionResponse resp    (.execute tx client)
        ^TransactionRecord tx-rec    (.getRecord resp client)
        ^TransactionReceipt receipt  (.getReceipt resp client)
        ^AccountId new-acct-id       (.-accountId receipt)]

    (println "Created account: " new-acct-id)

    new-acct-id))

(defn delete-acct [acct-id acct-priv-key admin-acct-id client]
  (let [^AccountDeleteTransaction tx (-> (AccountDeleteTransaction.)
                                         (.setAccountId acct-id)
                                         (.setTransferAccountId admin-acct-id))

        ^TransactionResponse resp    (-> tx
                                         (.freezeWith client)
                                         (.sign acct-priv-key)
                                         (.execute client))

        ^TransactionReceipt receipt  (.getReceipt resp client)
        ^Status status               (.-status receipt)]
    (println "Acct delete status: " status)
    status))

(defn sha256 [string]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") (.getBytes string "UTF-8"))]
    (apply str (map (partial format "%02x") digest))))



