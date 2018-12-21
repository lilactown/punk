(ns punk.ui.core
  (:require [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :refer [<-deref]]
            ["react-dom" :as react-dom]
            ["react-grid-layout" :as GridLayout]
            [goog.object :as gobj]
            [clojure.string :as s]
            [clojure.core.async :as a]
            [frame.core :as f]))

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
;; UI state
;;

(defonce ui-db (atom {:entries []
                      :history []
                      :current nil
                      :loading-next false
                      :next nil}))

(defonce ui-frame (f/create-frame
                   (f/inject-cofx :db)))

(defonce dispatch #(f/dispatch ui-frame %))

(f/reg-cofx
 ui-frame :db
 (fn db-cofx [cofx]
   (assoc cofx :db @ui-db)))

(f/reg-fx
 ui-frame :db
 (fn db-fx [v]
   (when (not (identical? @ui-db v))
     (reset! ui-db v))))

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
;; UI Events
;;

(f/reg-event-fx
 ui-frame :punk.ui.browser/view-entry
 [debug-db debug-event]
 (fn [{:keys [db]} [_ x]]
   {:db (assoc db
               :current x
               :next nil
               :loading-next false
               :history [])}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/view-next
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
 ui-frame :punk.ui.browser/history-back
 [#_debug-db debug-event]
 (fn [{:keys [db]} _]
   {:db (-> db
            (update :history pop)
            (assoc :current (-> db :history peek)
                   :next nil))}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/nav-to
 [#_debug-db debug-event]
 (fn [{:keys [db]} [_ idx k v]]
   {:db (assoc db :loading-next true)
    :emit [:nav idx k v]}))


;;
;; Punk events
;;

(f/reg-event-fx
 ui-frame :entry
 [#_debug-db debug-event]
 (fn [cofx [_ idx x]]
   {:db (update (:db cofx) :entries conj (assoc x :idx idx))}))

(f/reg-event-fx
 ui-frame :nav
 [debug-event debug-db]
 (fn [{:keys [db]} [_ idx x]]
   {:db (assoc db
               :loading-next false
               :next x)}))


(defnc MapView [{:keys [data on-next] :as props}]
  (when (not (nil? data))
    [:div (dissoc props :on-next :data)
     [:<>
      [:div {:style {:display "flex"
                     :border-bottom "1px solid #999"
                     :padding-bottom "3px"
                     :margin-bottom "3px"}}
       [:div {:style {:flex 1}} "key"]
       [:div {:style {:flex 2}} "value"]]
      (for [[key v] data]
        [:div {:style {:display "flex"}
               :key key
               :class "item"
               :on-click #(on-next data key v)}
         [:div {:style {:flex 1}}
          (prn-str key)]
         [:div {:style {:flex 2}}
          (prn-str v)]])]]))

(defnc CollView [{:keys [data on-next] :as props}]
  (when (not (nil? data))
    [:div (dissoc props :on-next :data)
     [:<>
      [:div {:style {:display "flex"
                     :border-bottom "1px solid #999"
                     :padding-bottom "3px"
                     :margin-bottom "3px"}}
       [:div {:style {:flex 1}} "idx"]
       [:div {:style {:flex 2}} "value"]]
      (for [[key v] (map-indexed vector data)]
        [:div {:style {:display "flex"}
               :key key
               :class "item"
               :on-click #(on-next data key v)}
         [:div {:style {:flex 1}}
          (prn-str key)]
         [:div {:style {:flex 2}}
          (prn-str v)]])]]))

(defnc SetView [{:keys [data on-next] :as props}]
  (when (not (nil? data))
    [:div (dissoc props :on-next :data)
     [:<>
      [:div {:style {:display "flex"
                     :border-bottom "1px solid #999"
                     :padding-bottom "3px"
                     :margin-bottom "3px"}}
       [:div {:style {:flex 2}} "value"]]
      (for [v (sort data)]
        [:div {:style {:display "flex"}
               :key v
               :class "item"
               :on-click #(on-next data key v)}
         [:div {:style {:flex 2}}
          (prn-str v)]])]]))

(defnc EdnView [{:keys [data on-next] :as props}]
  [:div (prn-str data)])

(def views
  [{:id :punk.view/nil
    :match nil?
    :view nil}

   {:id :punk.view/map
    :match map?
    :view MapView}

   {:id :punk.view/set
    :match set?
    :view SetView}

   {:id :punk.view/coll
    :match coll?
    :view CollView}

   {:id :punk.view/edn
    :match any?
    :view EdnView}])

(defn match-first-view [views data]
  (:view (first (filter #((:match %) data) views))))

;;
;; Browser panes
;;

(defnc Style [{:keys [children]}]
  [:style {:dangerouslySetInnerHTML #js {:__html (s/join "\n" children)}}])

(def layout
  #js [#js {:i "next" :x 0 :y 0 :w 12 :h 6}
       #js {:i "current" :x 0 :y 6 :w 12 :h 6}
       #js {:i "entries" :x 0 :y 12 :w 12 :h 6}])

(def GridLayoutWithWidth (GridLayout/WidthProvider GridLayout))

(defnc Browser [_]
  (let [state (<-deref ui-db)]
    [:div {:style {:height "100%"}
           :id "punk-container"}
     [Style
      "#punk-container {"
      "  font-family: sans-serif;"
      "  margin: 0;"
      "}"
      (str "#current { overflow: auto;"
           "       max-height: 100%;"
           "       padding: 10px"
           "}")
      (str "#current-grid {"
           "       box-shadow: 2px 2px 1px 1px #eee;"
           "       border: 1px solid #eee;"
           "}")
      "#current .item { cursor: pointer; padding: 3px; margin: 3px; }"
      "#current .item:hover { background-color: #eaeaea /*#44475a */; }"

      (str       "#next { overflow: auto;"
                 "       max-height: 100%;"
                "}")
      (str "#next-grid {"
           "       border: 1px solid #eee;"
           "       box-shadow: 2px 2px 1px 1px #eee;"
           "}")
      "#next { cursor: pointer; padding: 3px; margin: 3px; }"
      "#next:hover { background-color: #eaeaea /* #44475a */; }"
      "#next.nohover { cursor: initial; }"
      "#next.nohover:hover { background-color: initial; }"

      (str "#entries { overflow: auto;"
           "       max-height: 100%;"

           "}")
      (str "#entries-grid {"
           "       border: 1px solid #eee;"
           "       box-shadow: 2px 2px 1px 1px #eee;"
           "}")
      "#entries .item { cursor: pointer; padding: 3px 0; margin: 3px 0; }"
      "#entries .item:hover { background-color: #eaeaea /*#44475a */; }"]
     [GridLayoutWithWidth
      {:class "layout"
       :layout layout
       :cols 12
       :rowHeight 30}
      ;; Next
      [:div {:key "next" :id "next-grid"}
       [:h3 "Next"]
       [:div {:style {:display "flex"
                      :flex-direction "column"}}
        [(match-first-view views (-> state :next :value))
         {:data (-> state :next :value)
          :id "next"
          :on-next #(dispatch [:punk.ui.browser/view-next])}]]]
      ;; Current
      [:div {:key "current" :id "current-grid"}
       [:h3 "Current"]
       [:div {:style {:display "flex"
                      :flex-direction "column"}}
        [(match-first-view views (-> state :current :value))
         {:data (-> state :current :value)
          :id "current"
          :on-next #(dispatch [:punk.ui.browser/nav-to
                               (-> state :current :idx) %2 %3])}]]
       ;; Controls
       [:div
        [:button {:type "button"
                  :style {:width "60px"}
                  :disabled (empty? (:history state))
                  :on-click #(dispatch [:punk.ui.browser/history-back])} "<"]]];; Entrie
      [:div {:key "entries"
             :id "entries-grid"}
       [:div {:style {:overflow "auto"}
              :id "entries"}
        [:h3 {:style {:position "fixed" :top 0 :left 0 :right 0
                      ;; :background-color "white"
                      :margin-top 0
                      :padding "5px"
                      :margin-bottom 0}} "Entries"]
        [:div {:style {:margin-top "60px"}}
         (for [[idx entry] (reverse (map-indexed vector (:entries state)))]
           [:div {:on-click #(dispatch [:punk.ui.browser/view-entry entry])
                  :class "item"}
            idx " " (prn-str (:value entry))])]]]]]))

(defn start! []
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (a/go-loop []
      (let [ev (a/<! (gobj/get js/window "PUNK_IN_STREAM"))]
        (println ev)
        (dispatch ev)
        (recur)))
    (f/reg-fx
     ui-frame :emit
     (fn [v]
       (a/put! (gobj/get js/window "PUNK_OUT_STREAM") v)))
    (react-dom/render (hx/f [Browser]) container)))
