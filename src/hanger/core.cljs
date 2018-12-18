(ns hanger.core
  (:require [hanger.interceptors :as interceptor :refer [->interceptor]]
            [hanger.utils :refer [first-in-vector]]
            [hanger.interop :as interop :refer [empty-queue debug-enabled?
                                                set-timeout!]]
            [hanger.event-queue :as eq]
            [hanger.loggers :refer [console]]
            [hanger.std-interceptors :refer [fx-handler->interceptor]]))

;;
;; Handling handlers
;;

(def handler-kinds #{:event :fx :cofx :sub})

(defn- get-handler

  ([frame kind]
   (get @(:registrar frame) kind))

  ([frame kind id]
   (-> (get @(:registrar frame) kind)
       (get id)))

  ([frame kind id required?]
   (let [handler (get-handler frame kind id)]
     (when debug-enabled?
       ;; This is in a separate `when` so Closure DCE can run ...
       (when (and required? (nil? handler))
         ;; ...otherwise you'd need to type-hint the `and`
         ;; with a ^boolean for DCE.
         (console :error "hanger: no" (str kind) "handler registered for:" id)))
     handler)))

(defn- register-handler [frame kind id handler-fn]
  (when debug-enabled?
    ;; This is in a separate when so Closure DCE can run
    (when (get-handler frame kind id false)
      ;; allow it, but warn. Happens on hot reloads.
      (console :warn "hanger: overwriting" (str kind) "handler for:" id)))
  (swap! (:registrar frame) assoc-in [kind id] handler-fn)
  handler-fn)

(defn- clear-handlers
  ([frame]
   ;; clear all kinds
   (reset! (:registrar frame) {}))

  ([frame kind]
   ;; clear all handlers for this kind
   (assert (handler-kinds kind))
   (swap! (:registrar frame) dissoc kind))

  ([frame kind id]
   ;; clear a single handler for a kind
   (assert (handler-kinds kind))
   (if (get-handler kind id)
     (swap! (:registrar frame) update-in [kind] dissoc id)
     (console :warn "hanger: can't clear" (str kind) "handler for"
              (str id ". Handler not found.")))))


;;
;; Dispatching
;;

(def ^:dynamic *handling* nil)    ;; remember what event we are currently handling

(defn- handle-event
  "Given an event vector `event-v`, look up the associated interceptor chain, and execute it."
  [frame event-v]
  (let [event-id  (first-in-vector event-v)]
    (if-let [interceptors  (get-handler frame :event event-id true)]
      (if *handling*
        (console :error "hanger: while handling" *handling* ", dispatch-sync was called for" event-v ". You can't call dispatch-sync within an event handler.")
        (binding [*handling*  event-v]
          (interceptor/execute event-v interceptors))))))

(defn dispatch
  "Enqueue `event` for processing by event handling machinery.
  `event` is a vector of length >= 1. The 1st element identifies the kind of event.
  Note: the event handler is not run immediately - it is not run
  synchronously. It will likely be run 'very soon', although it may be
  added to the end of a FIFO queue which already contain events.
  Usage:
     (dispatch [:order-pizza {:supreme 2 :meatlovers 1 :veg 1})"
  [{:keys [event-queue]} event]
  (if (nil? event)
    (throw (ex-info "hanger: you called \"dispatch\" without an event vector." {}))
    (eq/push event-queue event))
  nil)

(defn dispatch-sync
  "Synchronously (immediately) process `event`. Do not queue.
  Generally, don't use this. Instead use `dispatch`. It is an error
  to use `dispatch-sync` within an event handler.
  Useful when any delay in processing is a problem:
     1. the `:on-change` handler of a text field where we are expecting fast typing.
     2  when initialising your app - see 'main' in todomvc examples
     3. in a unit test where we don't want the action 'later'
  Usage:
     (dispatch-sync [:sing :falsetto 634])"
  [{:keys [event-queue] :as frame} event-v]
  (handle-event frame event-v)
  ;; slightly ugly hack. Run the registered post event callbacks.
  (eq/-call-post-event-callbacks event-queue event-v)  
  ;; Ensure nil return. See https://github.com/Day8/re-frame/wiki/Beware-Returning-False
  nil)


;;
;; Coeffects
;;

(defn reg-cofx
  "Register the given coeffect `handler` for the given `id`, for later use
  within `inject-cofx`.
  `id` is keyword, often namespaced.
  `handler` is a function which takes either one or two arguements, the first of which is
  always `coeffects` and which returns an updated `coeffects`.
  See the docs for `inject-cofx` for example use."
  [frame id handler]
  (register-handler frame :cofx id handler))

(defn inject-cofx
  "Given an `id`, and an optional, arbitrary `value`, returns an interceptor
   whose `:before` adds to the `:coeffects` (map) by calling a pre-registered
   'coeffect handler' identified by the `id`.
   The previous association of a `coeffect handler` with an `id` will have
   happened via a call to `hanger.core/reg-cofx` - generally on program startup.
   Within the created interceptor, this 'looked up' `coeffect handler` will
   be called (within the `:before`) with two arguments:
     - the current value of `:coeffects`
     - optionally, the originally supplied arbitrary `value`
   This `coeffect handler` is expected to modify and return its first, `coeffects` argument.
   Example Of how `inject-cofx` and `reg-cofx` work together
   ---------------------------------------------------------
   1. Early in app startup, you register a `coeffect handler` for `:datetime`:
      (hanger.core/reg-cofx
        :datetime                        ;; usage  (inject-cofx :datetime)
        (fn coeffect-handler
          [coeffect]
          (assoc coeffect :now (js/Date.))))   ;; modify and return first arg
   2. Later, add an interceptor to an -fx event handler, using `inject-cofx`:
      (hanger.core/reg-event-fx        ;; we are registering an event handler
         :event-id
         [ ... (inject-cofx :datetime) ... ]    ;; <-- create an injecting interceptor
         (fn event-handler
           [coeffect event]
           ... in here can access (:now coeffect) to obtain current datetime ... )))
   Background
   ----------
   `coeffects` are the input resources required by an event handler
   to perform its job. The two most obvious ones are `db` and `event`.
   But sometimes an event handler might need other resources.
   Perhaps an event handler needs a random number or a GUID or the current
   datetime. Perhaps it needs access to a DataScript database connection.
   If an event handler directly accesses these resources, it stops being
   pure and, consequently, it becomes harder to test, etc. So we don't
   want that.
   Instead, the interceptor created by this function is a way to 'inject'
   'necessary resources' into the `:coeffects` (map) subsequently given
   to the event handler at call time."
  ([id]
   (fn [frame]
     (->interceptor
      :id      :coeffects
      :before  (fn coeffects-before
                 [context]
                 (if-let [handler (get-handler frame :cofx id)]
                   (update context :coeffects handler)
                   (console :error "No cofx handler registered for" id))))))
  ([id value]
   (fn [frame]
     (->interceptor
      :id     :coeffects
      :before  (fn coeffects-before
                 [context]
                 (if-let [handler (get-handler frame :cofx id)]
                   (update context :coeffects handler value)
                   (console :error "No cofx handler registered for" id)))))))


;;
;; Effects
;;

(defn reg-fx
  "Register the given effect `handler` for the given `id`.
  `id` is keyword, often namespaced.
  `handler` is a side-effecting function which takes a single argument and whose return
  value is ignored.
  Example Use
  -----------
  First, registration ... associate `:effect2` with a handler.
  (reg-fx
     :effect2
     (fn [value]
        ... do something side-effect-y))
  Then, later, if an event handler were to return this effects map ...
  {...
   :effect2  [1 2]}
   ... then the `handler` `fn` we registered previously, using `reg-fx`, will be
   called with an argument of `[1 2]`."
  [frame id handler]
  (register-handler frame :fx id handler))

(defn do-fx
  "An interceptor whose `:after` actions the contents of `:effects`. As a result,
  this interceptor is Domino 3.
  This interceptor is silently added (by reg-event-fx etc) to the front of
  interceptor chains for all events.
  For each key in `:effects` (a map), it calls the registered `effects handler`
  (see `reg-fx` for registration of effect handlers).
  So, if `:effects` was:
      {:dispatch  [:hello 42]
       :db        {...}
       :undo      \"set flag\"}
  it will call the registered effect handlers for each of the map's keys:
  `:dispatch`, `:undo` and `:db`. When calling each handler, provides the map
  value for that key - so in the example above the effect handler for :dispatch
  will be given one arg `[:hello 42]`.
  You cannot rely on the ordering in which effects are executed."
  [frame]
  (->interceptor
   :id :do-fx
   :after (fn do-fx-after
            [context]
            (doseq [[effect-key effect-value] (:effects context)]
              (if-let [effect-fn (get-handler frame :fx effect-key false)]
                (effect-fn effect-value)
                (console :error "hanger: no handler registered for effect:" effect-key ". Ignoring."))))))


(defn register-default-fx [frame]
  (reg-fx
   frame
   :dispatch-later
   (fn [value]
     (doseq [{:keys [ms] :as effect} (remove nil? value)]
       (if (or (empty? (:dispatch effect)) (not (number? ms)))
         (console :error "hanger: ignoring bad :dispatch-later value:" effect)
         (set-timeout! #(dispatch frame (:dispatch effect)) ms)))))
  (reg-fx
   frame
   :dispatch
   (fn [value]
     (if-not (vector? value)
       (console :error "hanger: ignoring bad :dispatch value. Expected a vector, but got:" value)
       (dispatch frame value))))
  (reg-fx
   frame
   :dispatch-n
   (fn [value]
     (if-not (sequential? value)
       (console :error "hanger: ignoring bad :dispatch-n value. Expected a collection, got got:" value)
       (doseq [event (remove nil? value)] (dispatch frame event)))))
  (reg-fx
   frame
   :deregister-event-handler
   (fn [value]
     (let [clear-event (partial clear-handlers frame :event)]
       (if (sequential? value)
         (doseq [event value] (clear-event event))
         (clear-event value))))))


;;
;; Events
;;

(defn- flatten-and-remove-nils
  "`interceptors` might have nested collections, and contain nil elements.
  return a flat collection, with all nils removed.
  This function is 9/10 about giving good error messages."
  [id interceptors]
  (let [make-chain  #(->> % flatten (remove nil?))]
    (if-not debug-enabled?
      (make-chain interceptors)
      (do
        ;; do a whole lot of development time checks
        (when-not (coll? interceptors)
          (console :error "hanger: when registering" id ", expected a collection of interceptors, got:" interceptors))
        (let [chain (make-chain interceptors)]
          (when (empty? chain)
            (console :error "hanger: when registering" id ", given an empty interceptor chain"))
          (when-let [not-i (first (remove interceptor/interceptor? chain))]
            (if (fn? not-i)
              (console :error "hanger: when registering" id ", got a function instead of an interceptor. Got:" not-i)
              (console :error "hanger: when registering" id ", expected interceptors, but got:" not-i)))
          chain)))))

(defn reg-event-fx
  "Register the given event `handler` (function) for the given `id`. Optionally, provide
  an `interceptors` chain.
  `id` is typically a namespaced keyword  (but can be anything)
  `handler` is a function: (coeffects-map event-vector) -> effects-map
  `interceptors` is a collection of interceptors. Will be flattened and nils removed.
  `handler` is wrapped in its own interceptor and added to the end of the interceptor
   chain, so that, in the end, only a chain is registered.
   Special effects and coeffects interceptors are added to the front of the
   interceptor chain.  These interceptors inject the value of app-db into coeffects,
   and, later, action effects."
  ([frame id handler]
   (reg-event-fx frame id nil handler))
  ([frame id interceptors handler]
   (register-handler
    frame
    :event id
    (flatten-and-remove-nils
     id
     [(map #(% frame) (:interceptors frame)) ;; default interceptors
      (do-fx frame)
      interceptors
      (fx-handler->interceptor handler)]))))


;;
;; Creating a new frame
;;

(defn create-frame [& default-interceptors]
  (let [frame {:registrar (atom {:event {}
                                 :fx {}
                                 :cofx {}})
               :interceptors default-interceptors}]
    (register-default-fx frame)
    (assoc frame
           :event-queue (eq/event-queue (partial handle-event frame)))))

#_(def frame (create-frame (inject-cofx :db)))

#_(reg-event-fx frame :foo (fn [cofx ev]
                             (js/console.log cofx ev)
                             {:db {:asdf "bar" }}))

#_(def app-db (atom {}))

#_(reg-cofx frame :db (fn [cofx] (assoc cofx :db @app-db)))

#_(reg-fx frame :db (fn [value] (reset! app-db value)))

#_(dispatch frame [:foo])

;;
;; -- Event Processing Callbacks  ---------------------------------------------
;;

(defn add-post-event-callback
  "Registers a function `f` to be called after each event is processed
   `f` will be called with two arguments:
    - `event`: a vector. The event just processed.
    - `queue`: a PersistentQueue, possibly empty, of events yet to be processed.
   This is useful in advanced cases like:
     - you are implementing a complex bootstrap pipeline
     - you want to create your own handling infrastructure, with perhaps multiple
       handlers for the one event, etc.  Hook in here.
     - libraries providing 'isomorphic javascript' rendering on  Nodejs or Nashorn.
  'id' is typically a keyword. Supplied at \"add time\" so it can subsequently
  be used at \"remove time\" to get rid of the right callback.
  "
  ([frame f]
   (add-post-event-callback f f))   ;; use f as its own identifier
  ([frame id f]
   (eq/add-post-event-callback (:event-queue frame) id f)))


(defn remove-post-event-callback
  [frame id]
  (eq/remove-post-event-callback (:event-queue frame) id))
