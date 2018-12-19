(ns punk.core
  (:require [goog.object :as gobj]
            [clojure.string :as s]
            [clojure.datafy :as d]
            [clojure.core.protocols :as p]
            [frame.core :as f]))

(def dbg> (partial js/console.log "punk>"))

;;
;; Implement general protocols
;;

(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))

(defn dataficate [x]
  (cond
    (object? x)
    (do (specify! x
          p/Datafiable
          (datafy [o] (dissoc (js->clj o)
                              ;; lol gross
                              "clojure$core$protocols$Datafiable$"
                              "clojure$core$protocols$Datafiable$datafy$arity$1")))
        x)

    :else x))

;;
;; Data structures
;;

(defprotocol WithIndex
  (with-index [this]))

(extend-protocol WithIndex
  cljs.core/PersistentVector
  (with-index [v] (map-indexed vector v))

  cljs.core/PersistentHashSet
  (with-index [s] (map-indexed vector s))

  cljs.core/List
  (with-index [s] (map-indexed vector s))

  cljs.core/LazySeq
  (with-index [s] (map-indexed vector s))

  default
  (with-index [x] x))

;;
;; App state
;;

(defonce punk-db (atom {:entries []
                        :history []
                        :current nil
                        :next {:coll nil
                               :k nil
                               :v nil}}))

(defonce punk-frame (f/create-frame
                    (f/inject-cofx :db)))

(f/reg-cofx
 punk-frame :db
 (fn db-cofx [cofx]
   (assoc cofx :db @punk-db)))

(f/reg-fx
 punk-frame :db
 (fn db-fx [v]
   (when (not (identical? @punk-db v))
     (reset! punk-db v))))

(defn dbg [f]
  (fn [x]
    (f x)
    x))

(def debug
  (frame.interceptors/->interceptor
   :id :punk/debug
   :before (dbg (partial js/console.log "before> "))
   :after (dbg (partial js/console.log "after> "))))


;;
;; Punk events
;;

(f/reg-event-fx
 punk-frame :punk/add-entry
 [debug]
 (fn [cofx [_ x]]
   {:db (update (:db cofx) :entries conj x)}))

#_(f/dispatch punk-frame [:punk/add-entry "foo"])

;;
;; External events and subscriptions
;;

(defn tap-fn [x] (f/dispatch punk-frame [:punk/add-entry x]))

(defn add-taps! []
  (add-tap tap-fn))

(defn remove-taps! []
  (remove-tap tap-fn))

#_(add-taps!)

#_(tap> 2)

;;
;; Connection adapter
;;
