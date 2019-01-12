(ns punk.ui.remote
  (:require [punk.ui.core :as core]
            [clojure.core.async :as a]
            [cljs.tools.reader.edn :as edn]
            [frame.core :as f]
            [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :as hooks :refer [<-deref <-effect]]
            ["react-dom" :as react-dom]))

(defonce in-chan (a/chan))

(defonce out-chan (a/chan))

(defonce subscriber core/external-handler)

(f/reg-fx
 core/ui-frame :emit
 (fn [v]
   (a/put! out-chan (pr-str v))))

;; subscriber loop
(a/go-loop []
  (let [ev (a/<! in-chan)]
    (subscriber ev)
    (recur)))

(defonce state (atom {:status :closed}))

(defn connect [{:keys [port]}]
  (let [conn (js/WebSocket. "ws://localhost:9876/ws")]
    ;; websocket config
    (.addEventListener conn "open"
                       (fn [ev]
                         (println "Open ")
                         (swap! state assoc :status :open)))
    (.addEventListener conn "message"
                       (fn [ev]
                         (js/console.log (.-data ev))
                         (a/put! in-chan (.-data ev))))
    (.addEventListener conn "close"
                       (fn [ev]
                         (swap! state assoc :status :closed)))

    ;; send socket messages
    (a/go-loop []
      (let [ev (a/<! out-chan)]
        (.send conn ev)
        (recur)))

    (swap! state merge {:conn conn})))

(defn close []
  (when (:conn @state)
    (.close (:conn @state))))

(defnc Connection [_]
  (let [state (<-deref state)]
    (<-effect (fn []
                (connect {:port 9876})
                #(close)) [])
    (println "render")
    [:div {:style {:background-color "#eee"
                   :padding "10px"}}
     (prn-str state)]))

(defn start! []
  (println "Starting!")
  (let [container (or (. js/document getElementById "connection")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "connection")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
   (react-dom/render (hx/f [:<>
                             [Connection]
                             [core/JustBrowser]]) container)))
