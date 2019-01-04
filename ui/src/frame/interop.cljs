(ns frame.interop
  (:require [goog.async.nextTick]))

(def next-tick goog.async.nextTick)

(def empty-queue #queue [])

(def after-render next-tick)

;; Make sure the Google Closure compiler sees this as a boolean constant,
;; otherwise Dead Code Elimination won't happen in `:advanced` builds.
;; Type hints have been liberally sprinkled.
;; https://developers.google.com/closure/compiler/docs/js-for-compiler
(def ^boolean debug-enabled? "@define {boolean}" ^boolean goog/DEBUG)

(defn deref? [x]
  (satisfies? IDeref x))


(defn set-timeout! [f ms]
  (js/setTimeout f ms))

(defn now []
  (if (and
       (exists? js/performance)
       (exists? js/performance.now))
    (js/performance.now)
    (js/Date.now)))

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
