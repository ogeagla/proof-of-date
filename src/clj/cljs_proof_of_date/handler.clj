(ns cljs-proof-of-date.handler
  (:require
    [compojure.core :refer [DELETE GET POST context defroutes]]
    [ring.middleware.cors :refer [wrap-cors]]
    [ring.middleware.json :refer [wrap-json-response wrap-json-body]]
    [compojure.route :refer [resources not-found]]
    [ring.util.response :refer [response content-type resource-response file-response]]
    [ring.middleware.reload :refer [wrap-reload]]
    [shadow.http.push-state :as push-state]
    [throttle.core :as throttle])
  (:import (java.util Date)))



(def ^:private home-throttle
  (throttle/make-throttler
    :unlock-proof-throttle
    :attempts-threshold 10
    :attempt-ttl-ms 1000))

(defroutes
  routes

  (GET "/" []
    (fn [req]
      (println (Date.) " Get home page: " {:ip (:remote-addr req)})
      (throttle/check home-throttle {:ip (:remote-addr req)})
      (resource-response "index.html" {:root "public"})))
  (GET "/health" [] "OK")

  (resources "/")

  (not-found
    (fn [handler]
      (println :404 (:remote-addr handler) (:uri handler))
      "<h1>Page not found</h1>")))


(def handler
  (do
    (-> routes
        wrap-json-body
        wrap-json-response

        (wrap-cors :access-control-allow-origin [#".*"] :access-control-allow-methods [:get :post :delete]))))


(def dev-handler
  (-> #'handler wrap-reload push-state/handle))


