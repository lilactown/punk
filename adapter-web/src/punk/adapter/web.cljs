(ns punk.adapter.web
  (:require [goog.object :as gobj]
            [punk.core :as punk]
            [frame.core :as f]
            [cljs.tools.reader.edn :as edn]
            [clojure.core.async :as a]))

(defonce in-chan (a/chan))

(defonce out-chan (a/chan))

;; Handles incoming events from the UI and passes them to the user-space frame
(defonce event-loop
  (a/go-loop []
    (let [ev (a/<! out-chan)]
      (punk/dispatch (edn/read-string ev))
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
  "https://github.com/Lokeh/punk/releases/download/0.0.2-alpha.2/punk.js")

(def default-css
  ["https://github.com/Lokeh/punk/releases/download/0.0.2-alpha.2/resizable.css"
   "https://github.com/Lokeh/punk/releases/download/0.0.2-alpha.2/grid-layout.css"])

(defn ^{:export true}
  start
  ([] (start nil))
  ([opts]
   (let [{:keys [ui/script ui/css]
          :or {script default-script css default-css}} opts]
     (if (. js/document getElementById "punk")
       (start-ui! opts)

       ;; first time running
       (let [new-container (. js/document createElement "div")
             script-tag (. js/document createElement "script")]

         ;; script tag
         (. script-tag setAttribute "src" script)

         (set! (.-onload ^js script-tag)
               (fn []
                 (start-ui! opts)))
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
