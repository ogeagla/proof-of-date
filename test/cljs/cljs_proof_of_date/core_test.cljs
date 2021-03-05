(ns cljs-proof-of-date.core-test
  (:require [cljs.test :refer-macros [deftest testing is]]
            [cljs-proof-of-date.core :as core]))

(deftest fake-test
  (testing "fake description"
    (is (= 1 2))))
