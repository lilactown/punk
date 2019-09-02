(ns punk.core
  (:require [frame.core :as f]
            [clojure.core.protocols :as p]
            [clojure.datafy :as d]
            #?(:cljs [cljs.repl :as repl])))

(def dbg>
  (partial #?(:clj println
              :cljs js/console.log)
           "punk>"))

;;
;; Implement general protocols
;;

(def datafy d/datafy)

(def nav d/nav)

#?(:cljs (extend-protocol IPrintWithWriter
           js/Symbol
           (-pr-writer [sym writer _]
             (-write writer (str "\"" (.toString sym) "\"")))))

(defn dataficate [x]
  #?(:clj x ; XXX: might need some work :)
     :cljs (cond
             ;; (object? x)
             ;; (do (specify! x
             ;;       p/Datafiable
             ;;       (datafy [o] (dissoc (js->clj o)
             ;;                           ;; lol gross
             ;;                           "clojure$core$protocols$Datafiable$"
             ;;                           "clojure$core$protocols$Datafiable$datafy$arity$1")))
             ;;     x)
             (instance? js/Error x)
             (repl/Error->map x)
             :else x)))

;;
;; State
;;

(defonce db (atom {:entries []}))

(defonce frame (f/create-frame (f/inject-cofx :db)))

(defonce dispatch #(f/dispatch frame %))

(f/reg-cofx
 frame :db
 (fn db-cofx [cofx]
   (assoc cofx :db @db)))

(f/reg-fx
 frame :db
 (fn db-fx [v]
   (when (not (identical? @db v))
     (reset! db v))))

(defn dbg [f]
  (fn [x]
    (f x)
    x))

(def debug-db
  (frame.interceptors/->interceptor
   :id :punk/debug-db
   :before (dbg (fn [x] (#?(:clj println
                            :cljs js/console.log)
                         "db/before> " (-> x :coeffects :db))))
   :after (dbg (fn [x] (#?(:clj println
                           :cljs js/console.log)
                        "db/after> " (-> x :effects :db))))))

(def debug-event
  (frame.interceptors/->interceptor
   :id :punk/debug-event
   :before (dbg (fn [x] (#?(:clj println
                            :cljs js/console.log)
                         "event> " (-> x :coeffects :event))))))

(def debug-fx
  (frame.interceptors/->interceptor
   :id :punk/debug-event
   :after (dbg (fn [x] (#?(:clj println
                           :cljs js/console.log)
                        "effects> " (-> x :effects))))))

(f/reg-event-fx
 frame :tap
 []
 (fn [{:keys [db]} [_ x]]
   (let [db' (update db :entries conj x)
         idx (count (:entries db))
         dx (datafy x)]
     {:db db'
      :emit [:entry idx {:value dx
                         :meta (meta dx)}]})))

(f/reg-event-fx
 frame :list
 []
 (fn [{:keys [db]} [_ x]]
   {:emit [:entries (->> (:entries db)
                         (mapv datafy)
                         (mapv (fn [dx] {:value dx
                                         :meta (meta dx)})))]}))

(f/reg-event-fx
 frame :nav
 []
 (fn [{:keys [db]} [_ idx k v]]
   (let [x (get-in db [:entries idx])
         ;; nav to next item in datafied object
         x' (nav (datafy x) k v)
         ;; store this nav'd value in db for reference later
         db' (update db :entries conj x')
         idx' (count (:entries db))
         dx' (datafy x')]
     {:db db'
      :emit [:nav idx {:value dx'
                       :meta (meta dx')
                       :idx idx'}]})))

(f/reg-event-fx
 frame :clear
 []
 (fn [{:keys [db]} _]
   {:db (assoc db :entries [])}))

(defonce tap-fn (fn tap-fn [x] (dispatch [:tap (dataficate x)])))

(defn remove-taps! []
  (remove-tap tap-fn))

(defn add-taps! []
  (remove-taps!)
  (add-tap tap-fn))
