(ns punk.adapter.web
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [cljs.tools.reader.edn :as edn]
            [clojure.core.async :as a]))

(defonce in-stream (a/chan))

(defonce out-stream (a/chan))

(defonce in-loop
  (a/go-loop []
    (let [ev (a/<! out-stream)]
      (punk/dispatch (edn/read-string ev))
      (recur))))

(f/reg-fx
 punk/frame :emit
 (fn [v]
   (a/put! in-stream (pr-str v))))

(punk/add-taps!)

(def start-ui!
  (gobj/getValueByKeys js/window "punk" "ui" "core" "start_BANG_"))

(defn ^{:export true}
  start []
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (start-ui! container in-stream out-stream)))
