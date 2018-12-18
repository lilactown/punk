(ns punk.core
  (:require [goog.object :as gobj]
            [clojure.string :as s]
            [clojure.datafy :as d]
            [clojure.core.protocols :as p]
            [frame.core]))

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
