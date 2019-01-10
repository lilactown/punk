(ns punk.ui.remote
  (:require [punk.ui.core :as core]
            [clojure.core.async :as a]
            [cljs.tools.reader.edn :as edn]))

(defonce ^:dynamic *conn* (atom nil))

(defonce in-chan (a/chan))

(defonce out-chan (a/chan))

(defonce subscriber (atom nil))

(defonce subscriber-loop
  (a/go-loop []
    (let [ev (a/<! in-chan)]
      (@subscriber ev)
      (recur))))

(defonce event-loop
  (a/go-loop []
    (let [ev (a/<! out-chan)]
      (.send @*conn* ev)
      (recur))))

(defn connect []
  (let [conn (js/WebSocket. "ws://localhost:9876/ws")]
    (.addEventListener conn "open"
                       (fn [ev]
                         (println "Opened!")))
    (.addEventListener conn "message"
                       (fn [ev]
                         (js/console.log (.-data ev))
                         (a/put! in-chan (.-data ev))))
    (.addEventListener conn "close"
                       (fn [ev]
                         (println "Closed!")))
    conn))

(def in-stream #js {:subscribe #(reset! subscriber %)
                    :unsubscribe #(reset! subscriber %)})

(def out-stream #js {:put (fn [v] (a/put! out-chan v))})

(defn start! []
  (println "Starting!")
  (when @*conn* (.close @*conn*))
  (reset! *conn* (connect))
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (core/start! container in-stream out-stream (prn-str {:drawer? false}))))
