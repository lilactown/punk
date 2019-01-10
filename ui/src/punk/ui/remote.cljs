(ns punk.ui.remote
  (:require [punk.ui.core :as core]
            [clojure.core.async :as a]
            [cljs.tools.reader.edn :as edn]
            [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :as hooks :refer [<-deref]]
            ["react-dom" :as react-dom]))

(defonce state (atom {:status :closed}))

(defn connect [{:keys [port]}]
  (let [conn (js/WebSocket. "ws://localhost:9876/ws")
        subscriber (atom nil)
        in-chan (a/chan)
        out-chan (a/chan)]
    ;; subscriber loop
    (a/go-loop []
      (let [ev (a/<! in-chan)]
        (@subscriber ev)
        (recur)))

    ;; send socket messages
    (a/go-loop []
      (let [ev (a/<! out-chan)]
        (.send conn ev)
        (recur)))

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
    (swap! state merge {:in-chan in-chan
                        :out-chan out-chan
                        :subscriber subscriber
                        :conn conn})))

(defn close [conn]
  (when conn
    (.close conn)))

(defnc Connection [_]
  (let [state (<-deref state)]
    (println "render")
    [:div {:style {:background-color "#eee"
                   :padding "10px"}}
     (prn-str state)]))


(defn start! []
  (println "Starting!")
  (close (:conn @state))
  (let [container (or (. js/document getElementById "connection")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "connection")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (react-dom/render (hx/f [Connection]) container))
  (connect {:port 9876})
  ;; (let [container (or (. js/document getElementById "punk")
  ;;                     (let [new-container (. js/document createElement "div")]
  ;;                       (. new-container setAttribute "id" "punk")
  ;;                       (-> js/document .-body (.appendChild new-container))
  ;;                       new-container))
  ;;       in-stream #js {:subscribe #(swap! state update :subscriber reset! %)
  ;;                      :unsubscribe #(swap! state update :subscriber reset! nil)}

  ;;       out-stream #js {:put (fn [v] (a/put! (:out-chan @state) v))}]
  ;;   (core/start! container in-stream out-stream (prn-str {:drawer? false})))
  )
