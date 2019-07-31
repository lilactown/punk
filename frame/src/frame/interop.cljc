(ns frame.interop
  #?(:cljs (:require [goog.async.nextTick]))
  #?(:clj (:import [java.util.concurrent Executor Executors])))

#?(:clj
   (defonce ^:private executor (Executors/newSingleThreadExecutor)))

(def next-tick
  #?(:clj (fn [f]
            (let [bound-f (bound-fn [& args] (apply f args))]
              (.execute ^Executor executor bound-f))
            nil)
     :cljs goog.async.nextTick))

(def empty-queue
  #?(:clj clojure.lang.PersistentQueue/EMPTY
     :cljs #queue []))

(def after-render next-tick)

#?(:clj
   (def debug-enabled? true)
   :cljs
   ;; Make sure the Google Closure compiler sees this as a boolean constant,
   ;; otherwise Dead Code Elimination won't happen in `:advanced` builds.
   ;; Type hints have been liberally sprinkled.
   ;; https://developers.google.com/closure/compiler/docs/js-for-compiler
   (def ^boolean debug-enabled? "@define {boolean}" ^boolean
     false ;; this this off for now since it is so verbose when punk isn't connected
     #_goog/DEBUG))

(defn deref? [x]
  #?(:clj
     (instance? clojure.lang.IDeref x)
     :cljs
     (satisfies? IDeref x)))

(defn set-timeout! [f ms]
  #?(:clj
     ;; Note that we ignore the `ms` value and just invoke the
     ;; function, because there isn't often much point firing a timed
     ;; event in a test."
     (next-tick f)
     :cljs
     (js/setTimeout f ms)))

(defn now []
  #?(:clj
     ;; currentTimeMillis may count backwards in some scenarios, but
     ;; as this is used for tracing it is preferable to the slower but
     ;; more accurate System.nanoTime.
     (System/currentTimeMillis)
     :cljs
     (if (and
          (exists? js/performance)
          (exists? js/performance.now))
       (js/performance.now)
       (js/Date.now))))

;; (defn reagent-id
;;   "Produces an id for reactive Reagent values
;;   e.g. reactions, ratoms, cursors."
;;   [reactive-val]
;;   (when (implements? reagent.ratom/IReactiveAtom reactive-val)
;;     (str (condp instance? reactive-val
;;            reagent.ratom/RAtom "ra"
;;            reagent.ratom/RCursor "rc"
;;            reagent.ratom/Reaction "rx"
;;            reagent.ratom/Track "tr"
;;            "other")
;;          (hash reactive-val))))
