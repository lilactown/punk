(ns punk.adapter.node
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [cljs.tools.reader.edn :as edn]
            ["http" :as http]
            ["ws" :as ws]))

(defonce server (atom #js {:close (fn [])}))

(defn start []
  (let [http-server (.createServer http)
        ws-server (ws/Server. #js {:noServer true})]
    (punk/remove-taps!)
    (punk/add-taps!)

    ;; setup the websocket
    (.on ws-server "connection"
         (fn connection [ws]
           (js/console.log "opened")
           (f/reg-fx
            punk/frame :emit
            (fn emit [v]
              (.send ws (pr-str v))))
           (.on ws "message"
                (fn message [m]
                  (punk/dispatch (edn/read-string m))))
           (.on ws "close" (fn close [] (js/console.log "closed")))))

    ;; setup the http server
    (.on http-server "upgrade"
         (fn upgrade [req socket head]
           (if (= (.-url req) "/ws")
             (.handleUpgrade ws-server req socket head
                             (fn done [ws]
                               (.emit ws-server "connection" ws req)))
             (.destroy socket))))

    (.on http-server "close"
         (fn close []
           (.close ws-server)))

    (.listen http-server 9876)
    (reset! server http-server)))

(defn stop []
  (.close @server))

(println "hi2")

(defn -main []
  (start))

#_(stop)
#_(start)

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
