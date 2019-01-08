(ns punk.adapter.node
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [cljs.tools.reader.edn :as edn]
            ["ws" :as ws]))

(defonce ws-server (atom #js {:close (fn [])}))

(defn start []
  (let [-ws-server (ws/Server. #js {:port 9876})]
    (punk/remove-taps!)
    (punk/add-taps!)
    (.on -ws-server "connection"
         (fn [ws]
           (js/console.log "opened")
           (f/reg-fx
            punk/frame :emit
            (fn [v]
              (.send ws (pr-str v))))
           (.on ws "message"
                (fn [m]
                  (punk/dispatch (edn/read-string m))
                  (js/console.log m)))
           (.on ws "close" (fn [] (js/console.log "closed")))))
    (reset! ws-server -ws-server)))

(defn stop []
  (.close @ws-server))

(println "hi2")

(defn -main [])

#_(-stop)
#_(-start)

#_(tap> {:foo 'bar [1234] 4567})

;; (def start-ui!
;;   (gobj/getValueByKeys js/window "punk" "ui" "core" "start_BANG_"))

;; (def in-stream #js {:subscribe #(swap! subscribers conj %)
;;                     :unsubscribe #(swap! subscribers disj %)})

;; (def out-stream #js {:put (fn [v] (a/put! out-chan v))})

;; (defn ^{:export true}
;;   start
;;   ([] (start nil))
;;   ([opts]
;;    (let [container (or (. js/document getElementById "punk")
;;                        (let [new-container (. js/document createElement "div")]
;;                          (. new-container setAttribute "id" "punk")
;;                          (-> js/document .-body (.appendChild new-container))
;;                          new-container))]
;;      (punk/remove-taps!)
;;      (punk/add-taps!)
;;      (start-ui! container in-stream out-stream (pr-str opts)))))
