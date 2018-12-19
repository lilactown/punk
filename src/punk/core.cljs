(ns punk.core
  (:require [goog.object :as gobj]
            [clojure.string :as s]
            [clojure.datafy :as d]
            [clojure.core.protocols :as p]
            [frame.core :as f]
            [clojure.datafy :as d]))

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
;; App state
;;

(defonce punk-db (atom {:entries [{:value {:foo "bar"}
                               :datafied {:foo "bar"}}]
                    :history []
                    :current nil
                    :next nil}))

(defonce punk-frame (f/create-frame
                    (f/inject-cofx :db)))

(set! (.-PUNK_DB js/window) punk-db)

(set! (.-PUNK_FRAME js/window) punk-frame)

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

(def debug-db
  (frame.interceptors/->interceptor
   :id :punk/debug-db
   :before (dbg (fn [x] (js/console.log "db/before> " (-> x :coeffects :db))))
   :after (dbg (fn [x] (js/console.log "db/after> " (-> x :effects :db))))))

(def debug-event
  (frame.interceptors/->interceptor
   :id :punk/debug-event
   :before (dbg (fn [x] (js/console.log "event> " (-> x :coeffects :event))))))

;;
;; Punk events
;;

(f/reg-event-fx
 punk-frame :punk.browser/add-entry
 [#_debug-db debug-event]
 (fn [cofx [_ x]]
   {:db (update (:db cofx) :entries conj {:value x
                                          :datafied (d/datafy x)})}))

(f/reg-event-fx
 punk-frame :punk.browser/view-entry
 [#_debug-db debug-event]
 (fn [{:keys [db]} [_ x]]
   {:db (assoc db
               :current x
               :next nil
               :history [])}))

(f/reg-event-fx
 punk-frame :punk.browser/nav-to
 [#_debug-db debug-event]
 (fn [{:keys [db]} [_ coll k v]]
   (let [nv (d/nav coll k v)]
     {:db (-> db
              (assoc :next {:value nv
                            :datafied (d/datafy nv)}))})))

(f/reg-event-fx
 punk-frame :punk.browser/view-next
 [#_debug-db debug-event]
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc
             :current (:next db)
             :next nil)
            (update
             :history
             conj (:current db)))}))

(f/reg-event-fx
 punk-frame :punk.browser/history-back
 [#_debug-db debug-event]
 (fn [{:keys [db]} _]
   {:db (-> db
            (update :history pop)
            (assoc :current (-> db :history peek)
                   :next nil))}))

#_(f/dispatch punk-frame [:punk.browser/add-entry "foo"])

;;
;; External events and subscriptions
;;

(defn tap-fn [x] (f/dispatch punk-frame [:punk.browser/add-entry (dataficate x)]))

(defn remove-taps! []
  (remove-tap tap-fn))

(defn add-taps! []
  (remove-taps!)
  (add-tap tap-fn))
