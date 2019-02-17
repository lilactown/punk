(ns punk.adapter.jvm
  (:require
   [aleph.http :as ah]
   [clojure.edn :as ce]
   [clojure.java.io :as cji]
   [compojure.core :as cc]
   [compojure.route :as cr]
   [frame.core :as fc]
   [manifold.stream :as ms]
   [manifold.deferred :as md]
   [manifold.bus :as mb]
   [punk.core :as pc]
   [ring.middleware.params :as rmp]))

(def port
  9876)

(defonce server
  (atom nil))

(def default-script
  ["https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.7/ui/dist/js/main.js"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.7/ui/dist/js/remote.js"]
  ;; "http://localhost:8701/main.js"
  )

(def default-css
  ["https://fonts.googleapis.com/css?family=Source+Sans+Pro"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.7/ui/dist/css/grid-layout.css"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.7/ui/dist/css/resizable.css"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.7/ui/dist/css/punk.css"])

(defn css-tag [href]
  (str "<link href=\"" href "\" rel=\"stylesheet\" />"))

(defn script-tag [src]
  (str "<script src=\"" src "\" type=\"text/javascript\"></script>"))

(def page
  (str "<html>"
       " <head>"
       "<style>body { margin: 0 }</style>"
       (apply str
              (map css-tag default-css))
       " </head>"
       "<body>"
       (apply str (map script-tag default-script))
       "  <script>punk.ui.remote.start_BANG_()</script>"
       "</body>"
       "</html>"))

;; for alternative styles, see:
;;   https://github.com/ztellman/aleph/blob/master/examples/ \
;;           src/aleph/examples/websocket.clj
(defn ws-handler
  [req]
  (if-let [socket (try
                    @(ah/websocket-connection req)
                    (catch Exception e
                      nil))]
    (do
      (fc/reg-fx
       pc/frame :emit
       (fn emit [v]
         (ms/put! socket (pr-str v))))
      (md/loop []
        (let [msg @(ms/take! socket)
              read-value (ce/read-string
                          {;; XXX: fill in and uncomment as desired
                           #_#_:readers {}
                           :default tagged-literal}
                          msg)]
          (pc/dispatch read-value)
          (md/recur))))
    {:status 400
     :headers {"Content-Type" "application/text"}
     :body "Unexpected request"}))

(def handler
  (rmp/wrap-params
   (cc/routes
    (cc/GET "/" [] page)
    (cc/GET "/ws" [] ws-handler)
    (cr/not-found "Not found"))))

(defn start
  []
  (let [hs (ah/start-server handler
                            {:port port})]
    (reset! server hs)
    ;;(pc/remove-taps!) ; XXX: called by add-taps!
    (pc/add-taps!)))

(defn stop
  []
  ;; XXX: not working so well yet...
  ;;        https://github.com/ztellman/aleph/issues/365
  #_(.close @server))

(comment

  (defn send-msg
    [msg]
    (let [conn @(ah/websocket-client
                 (str "ws://localhost:" port "/ws"))]
      (ms/put! conn msg)))

  )
