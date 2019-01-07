(ns punk.adapter.web
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [cljs.tools.reader.edn :as edn]
            [clojure.core.async :as a]))

(defonce in-chan (a/chan))

(defonce out-chan (a/chan))

;; Handles incoming events from the UI and passes them to the user-space frame
(defonce event-loop
  (a/go-loop []
    (let [ev (a/<! out-chan)]
      (punk/dispatch (edn/read-string ev))
      (recur))))

(defonce subscribers (atom #{}))

(defonce subscriber-loop
  (a/go-loop []
    (let [ev (a/<! in-chan)]
      (doseq [sub-handler @subscribers]
        (sub-handler ev))
      (recur))))

(f/reg-fx
 punk/frame :emit
 (fn [v]
   (a/put! in-chan (pr-str v))))

(def start-ui!
  (gobj/getValueByKeys js/window "punk" "ui" "core" "start_BANG_"))

(def in-stream #js {:subscribe #(swap! subscribers conj %)
                    :unsubscribe #(swap! subscribers disj %)})

(def out-stream #js {:put (fn [v] (a/put! out-chan v))})

(defn ^{:export true}
  start
  ([] (start nil))
  ([opts]
   (let [container (or (. js/document getElementById "punk")
                       (let [new-container (. js/document createElement "div")]
                         (. new-container setAttribute "id" "punk")
                         (-> js/document .-body (.appendChild new-container))
                         new-container))]
     (punk/remove-taps!)
     (punk/add-taps!)
     (start-ui! container in-stream out-stream (pr-str opts)))))
