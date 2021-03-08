(ns cljs-proof-of-date.digest
  (:require [goog.crypt :as crypt])
  (:import goog.crypt.Sha256))


(defn bytes->hex
  "convert bytes to hex"
  [bytes-in]
  (crypt/byteArrayToHex bytes-in))

(defn get-sha256-str [input-str]
  (js/console.log "compute sha256 :" input-str)
  (let [sha        (Sha256.)
        _          (-> sha
                     (.update input-str))
        digest     (bytes->hex (.digest sha))]
    digest))
