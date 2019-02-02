(ns punk.adapter.node
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [cljs.tools.reader.edn :as edn]
            ["http" :as http]
            ["ws" :as ws]))

(defonce server (atom #js {:close (fn [])}))

(def default-script
  ["https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.3/ui/dist/js/main.js"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.3/ui/dist/js/remote.js"]
  ;; "http://localhost:8701/main.js"
  )

(def default-css
  ["https://fonts.googleapis.com/css?family=Source+Sans+Pro"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.3/ui/dist/css/grid-layout.css"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.3/ui/dist/css/resizable.css"])

(defn css-tag [href]
  (str "<link href=\"" href "\" rel=\"stylesheet\" />"))

(defn script-tag [src]
  (str "<script src=\"" src "\" type=\"text/javascript\"></script>"))

(def page
  (str "<html>"
       " <head>"
       (apply str
              (map css-tag default-css))
       " </head>"
       "<body>"
       (apply str (map script-tag default-script))
       "  <script>punk.ui.remote.start_BANG_()</script>"
       "</body>"
       "</html>"))

(defn handler [req res]
  (.end
   res
   page))

(defn start []
  (let [http-server (.createServer http handler)
        ws-server (ws/Server. #js {:noServer true})]
    (punk/remove-taps!)
    (punk/add-taps!)

    ;; setup the websocket
    (.on ws-server "connection"
         (fn connection [ws]
           (f/reg-fx
            punk/frame :emit
            (fn emit [v]
              (.send ws (pr-str v))))
           (.on ws "message"
                (fn message [m]
                  (punk/dispatch (edn/read-string m))))
           (.on ws "close" (fn close [] nil))))

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
