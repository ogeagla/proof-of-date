(ns cljs-proof-of-date.lib.digest
  (:require [goog.crypt :as crypt])
  (:import goog.crypt.Sha256))


(defn bytes->hex
  "convert bytes to hex"
  [bytes-in]
  (crypt/byteArrayToHex bytes-in))

(defn get-sha256-str [input-str]
  (if (or (nil? input-str)
          (= "" input-str))
    nil
    (let [sha    (Sha256.)
          _      (-> sha
                     (.update input-str))
          digest (bytes->hex (.digest sha))]
      digest)))
