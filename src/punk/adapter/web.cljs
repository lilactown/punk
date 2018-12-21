(ns punk.adapter.web
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [clojure.core.async :as a]))

(defonce in-stream (a/chan))

(defonce out-stream (a/chan))

(defonce in-loop
  (a/go-loop []
    (let [ev (a/<! out-stream)]
      (punk/dispatch ev)
      (recur))))

(f/reg-fx
 punk/frame :emit
 (fn [v]
   (a/put! in-stream v)))

(gobj/set js/window "PUNK_IN_STREAM" in-stream)

(gobj/set js/window "PUNK_OUT_STREAM" out-stream)

(punk/add-taps!)
