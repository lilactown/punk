(ns punk.adapter.web
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [clojure.core.async :as a]))

(def in-stream (a/chan))

(def out-stream (a/chan))

(def in-loop
  (a/go-loop []
    (let [ev (a/<! out-stream)]
      (punk/dispatch ev)
      (recur))))

(f/reg-fx
 punk/frame :emit
 (fn [v]
   (a/put! in-stream v)))

;; (gobj/set js/window "PUNK_IN_STREAM" in-stream)

;; (gobj/set js/window "PUNK_OUT_STREAM" out-stream)

(punk/add-taps!)

(def start-ui! (gobj/getValueByKeys js/window "punk" "ui" "core" "start_BANG_"))

(println in-stream)

(defn
  start []
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (start-ui! container {:foo "bar"} out-stream)))
