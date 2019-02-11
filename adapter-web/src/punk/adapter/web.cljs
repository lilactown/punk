(ns punk.adapter.web
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [cljs.tools.reader.edn :as edn]
            [cljs.tagged-literals]
            [clojure.core.async :as a]))

(defonce in-chan (a/chan))

(defonce out-chan (a/chan))

;; Handles incoming events from the UI and passes them to the user-space frame
(defonce event-loop
  (a/go-loop []
    (let [ev (a/<! out-chan)]
      (punk/dispatch
       (edn/read-string
        {:readers {;; 'js (with-meta identity {:punk/literal-tag 'js})
                   'inst cljs.tagged-literals/read-inst
                   'uuid cljs.tagged-literals/read-uuid
                   'queue cljs.tagged-literals/read-queue}
         :default tagged-literal}
        ev))
      (recur))))

(defonce subscribers (atom #{}))

(defonce subscriber-loop
  (a/go-loop []
    (let [ev (a/<! in-chan)]
      (doseq [sub-handler @subscribers]
        (sub-handler ev))
      (recur))))

(f/reg-fx
 punk/frame :emit
 (fn [v]
   (a/put! in-chan (pr-str v))))

(def in-stream #js {:subscribe #(swap! subscribers conj %)
                    :unsubscribe #(swap! subscribers disj %)})

(def out-stream #js {:put (fn [v] (a/put! out-chan v))})

(defn start-ui! [opts]
  (let [start! (gobj/getValueByKeys js/window "punk" "ui" "core" "start_BANG_")
        container (. js/document getElementById "punk")]
    (punk/remove-taps!)
    (punk/add-taps!)
    (start! container in-stream out-stream (pr-str opts))))


;;
;; ---- Defaults ----
;;

(def default-script
  "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.5/ui/dist/js/main.js"
  ;; "http://localhost:8701/main.js"
  )

(def default-css
  ["https://fonts.googleapis.com/css?family=Source+Sans+Pro"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.5/ui/dist/css/grid-layout.css"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.5/ui/dist/css/resizable.css"
   "https://cdn.jsdelivr.net/gh/Lokeh/punk@v0.0.5/ui/dist/css/punk.css"])

(defn ^{:export true}
  start
  ([] (start nil))
  ([opts]
   (let [default-opts {:ui/script default-script
                       :ui/css default-css}
         opts-with-defaults (merge default-opts opts)
         {:keys [ui/script ui/css]} opts-with-defaults]
     (if (. js/document getElementById "punk")
       (start-ui! opts-with-defaults)

       ;; first time running
       (let [new-container (. js/document createElement "div")
             script-tag (. js/document createElement "script")]

         ;; script tag
         (. script-tag setAttribute "src" script)

         (set! (.-onload ^js script-tag)
               (fn []
                 (start-ui! opts-with-defaults)))
         (-> js/document .-body (.appendChild script-tag))

         ;; css
         (doseq [sheet css]
           (let [link-tag (. js/document createElement "link")]
             (. link-tag setAttribute "rel" "stylesheet")
             (. link-tag setAttribute "type" "text/css")
             (. link-tag setAttribute "href" sheet)
             (-> js/document .-body (.appendChild link-tag))))

         ;; container
         (. new-container setAttribute "id" "punk")
         (-> js/document .-body (.appendChild new-container))
         new-container)))))
