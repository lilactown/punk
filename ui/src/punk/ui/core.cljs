(ns punk.ui.core
  (:require [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :refer [<-deref <-state <-effect <-ref]]
            ["react-dom" :as react-dom]
            ["react-grid-layout" :as GridLayout]
            [goog.object :as gobj]
            [clojure.string :as s]
            [clojure.core.async :as a]
            [cljs.tools.reader.edn :as edn]
            [frame.core :as f]
            [punk.ui.views :as views]
            [punk.ui.components :as pc]))

;;
;; Helpers
;;

;; Window-size hook based on https://github.com/rehooks/window-size/blob/master/index.js

(defn get-size [window]
  {:inner-height (.-innerHeight window)
   :inner-width (.-innerWidth window)
   :outer-height (.-outerHeight window)
   :outer-width (.-outerWidth window)})

(defn <-window-size
  ([] (<-window-size js/window))
  ([window]
   (let [window-size (<-state (when window (get-size window)))
         handle-resize #(reset! window-size (get-size window))]
     ;; Effect adds the event handler to the window resize event
     (<-effect
      (fn []
        (when window
          (.addEventListener window "resize" handle-resize)
          ;; return the unsubscribe function
          #(.removeEventListener window "resize" handle-resize)))
      ;; only re-sub on re-mount or new window val
      #js [window])

     ;; return value
     @window-size)))

(defn <-mouse-move [cb]
  (<-effect (fn []
              (.addEventListener js/window "mousemove" cb)
              #(.removeEventListener js/window "mousemove" cb))
            [cb]))

(defn <-mouse-up [cb]
  (<-effect (fn []
              (.addEventListener js/window "mouseup" cb)
              #(.removeEventListener js/window "mouseup" cb))
            [cb]))

;;
;; UI state
;;

(defonce ui-db (atom {:entries []
                      :history []
                      :current nil
                      :next/loading false
                      :next nil
                      :collapsed? true
                      :drawer-width 50
                      :views [{:id :punk.view/nil
                               :match nil?
                               :view nil}

                              {:id :punk.view/map
                               :match map?
                               :view #'views/MapView}

                              {:id :punk.view/set
                               :match set?
                               :view #'views/SetView}

                              {:id :punk.view/coll
                               :match (every-pred
                                       coll?
                                       (comp not map?))
                               :view #'views/CollView}

                              {:id :punk.view/edn
                               :match any?
                               :view #'views/EdnView}]
                      :view/selected nil}))

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
 ui-frame :punk.ui.drawer/toggle
 []
 (fn [{:keys [db]} _]
   {:db (update db :collapsed? not)}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/view-entry
 [#_debug-db #_debug-event]
 (fn [{:keys [db]} [_ x]]
   {:db (assoc db
               :current x
               :next nil
               :next/loading false
               :history [])}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/view-next
 [#_debug-db #_debug-event]
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
 [#_debug-db #_debug-event]
 (fn [{:keys [db]} _]
   {:db (-> db
            (update :history pop)
            (assoc :current (-> db :history peek)
                   :next nil
                   :view/selected nil))}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/nav-to
 [#_debug-db #_debug-event]
 (fn [{:keys [db]} [_ idx k v]]
   {:db (assoc db :next/loading true)
    :emit [:nav idx k v]}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/select-view-type
 []
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :view/selected id)}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/register-view
 [#_debug-db]
 (fn [{:keys [db]} [_ v]]
   {:db (update db :views conj v)}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/unregister-view
 []
 (fn [{:keys [db]} [_ id]]
   ;; filterv here is important to preserve order
   (let [views' (filterv #(not= id (:id %)) (:views db))]
     {:db (assoc db :views views')})))

(f/reg-event-fx
 ui-frame :punk.ui.drawer/change-width []
 (fn [{:keys [db]} [_ width]]
   {:db (assoc db :drawer-width width)}))

(defn register-view!
  [& {:keys [id match view] :as v}]
  (dispatch [:punk.ui.browser/register-view v]))

(defn unregister-view!
  [id]
  (dispatch [:punk.ui.browser/unregister-view id]))

(defn match-views [views data]
  (filter #((:match %) data) views))

;;
;; Punk events
;;

(f/reg-event-fx
 ui-frame :entry
 [#_debug-db #_debug-event]
 (fn [cofx [_ idx x]]
   {:db (update (:db cofx) :entries conj (assoc x :idx idx))}))

(f/reg-event-fx
 ui-frame :nav
 [#_debug-event #_debug-db]
 (fn [{:keys [db]} [_ idx x]]
   {:db (assoc db
               :next/loading false
               :next x
               :view/selected nil)}))

;;
;; Browser panes
;;

(def layout
  #js [#js {:i "next" :x 0 :y 0 :w 12 :h 6}
       #js {:i "current" :x 0 :y 6 :w 12 :h 6}
       #js {:i "entries" :x 0 :y 12 :w 12 :h 6}])

(def GridLayoutWithWidth (GridLayout/WidthProvider GridLayout))

(defnc Browser [{:keys [state width]}]
  (let [next-views (-> (:views state)
                       (match-views (-> state :next :value)))

        next-view (if (:view/selected state)
                    (first (filter #(= (:id %) (:view/selected state)) next-views))
                    (first next-views))
        current-view (-> (:views state)
                         (match-views (-> state :current :value))
                         (first))]
    [:div {:style {:height "100%"}
           :id "punk-container"}
     [pc/Style
      "#punk-container {"
      "  font-family: 'Source Sans Pro', sans-serif;"
      "  background: white;"
      "  margin: 0;"
      "}"
      (str "#current-grid {"
           "       box-shadow: 2px 2px 1px 1px #eee;"
           "       border: 1px solid #eee;"
           "}")
      "#current .item { cursor: pointer;}"
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
     [GridLayout
      {:class "layout"
       :layout layout
       :cols 12
       :rowHeight 30
       :width width
       :draggableHandle ".titlebar"}
      ;; Next
      [:div {:key "next"}
       [pc/Pane {:title "Next"}
        [:select {:value (str (:id next-view))
                  :on-change #(dispatch [:punk.ui.browser/select-view-type
                                         (keyword (subs (.. % -target -value) 1))])}
         (for [vid (map (comp str :id) next-views)]
           [:option {:key vid} vid])]
        [:div {:style {:display "flex"
                       :flex-direction "column"}}
         [(:view next-view)
          {:data (-> state :next :value)
           :id "next"
           :nav #(dispatch [:punk.ui.browser/view-next])}]]]]
      ;; Current
      [:div {:key "current"}
       [pc/Pane {:title "Current"
                 :id "current"
                 :controls [:div
                            [:button {:type "button"
                                      :style {:width "60px"}
                                      :disabled (empty? (:history state))
                                      :on-click #(dispatch [:punk.ui.browser/history-back])} "<"]]}
        [:div {:style {:display "flex"
                       :flex-direction "column"}}
         [(:view current-view)
          {:data (-> state :current :value)
           :nav #(dispatch [:punk.ui.browser/nav-to
                            (-> state :current :idx) %2 %3])}]]]]
      ;; Entries
      [:div {:key "entries"}
       [pc/Pane {:title "Entries" :id "entries"}
        (let [entries (reverse (map-indexed vector (:entries state)))]
          [pc/Table {:cols [[:id first {:flex 1}]
                            [:value (comp :value second) {:flex 11}]
                            ;; [:meta (comp :meta second) {:flex 5}]
                            ]
                     :on-entry-click (fn [_ entry]
                                       (dispatch [:punk.ui.browser/view-entry (second entry)]))
                     :data entries}])]]]]))

(def dragging? (atom false))

(defnc Drawer [_]
  (let [state (<-deref ui-db)
        collapsed? (:collapsed? state)
        win-size (<-window-size)
        move-handler #(when @dragging?
                          (dispatch
                           [:punk.ui.drawer/change-width (* 100
                                                            (/ (- (:inner-width win-size) (.. % -clientX))
                                                               (:inner-width win-size)))]))]
    (<-mouse-move move-handler)
    (<-mouse-up #(reset! dragging? false))
    [:div {:style {:position "absolute"
                   :width (if collapsed? "20px" (/ (:inner-width win-size)
                                                   (/ 100 (:drawer-width state))))
                   :top 0
                   :bottom 0
                   :right 0
                   :z-index 10}}
     [pc/Style
      "#punk__drawer-toggle {"
      " background: #f3f3f3;"
      " height: 100%;"
      " width: 20px;"
      " position: relative;"
      " border: 1px solid #eee;"
      "}"
      "#punk__drawer-toggle:hover {"
      " background: #ddd;"
      " cursor: pointer;"
      "}"
      "#punk__drawer-dragger { height: 100%; width: 3px; }"
      "#punk__drawer-dragger:hover { cursor: col-resize; }"]
     [:div {:style {:display "flex"}}
      (when-not collapsed?
        [:div {:id "punk__drawer-dragger"
               :on-mouse-down #(do
                                 (.preventDefault %)
                                 (reset! dragging? true))}])
      [:div {:id "punk__drawer-toggle"
             :on-click #(dispatch [:punk.ui.drawer/toggle])}
       [:div {:style {:position "absolute"
                      :text-align "center"
                      :left 0
                      :right 0
                      :top 10
                      :font-size "10px"}}
        (if collapsed? ">>" "<<")]
       [:div {:style {:position "absolute"
                      :text-align "center"
                      :left 0
                      :right 0
                      :bottom 10
                      :font-size "10px"}}
        (if collapsed? ">>" "<<")]]
      (when (not collapsed?)
        [:div {:style {:flex 1}}
         [Browser {:state state}]])]]))

(defnc JustBrowser [_]
  (let [state (<-deref ui-db)
        win-size (<-window-size)]
    [Browser {:state state :width (:inner-width win-size)}]))

(defn <-new-window []
  (let [win&container (<-state nil)]
    (<-effect
     (fn []
       (let [ext-window (.open js/window "" "" "width=800,height=800,left=200,top=200")
             container-el (-> ext-window .-document (.createElement "div"))]
         (-> ext-window .-document .-body (.appendChild container-el))
         (reset! win&container [ext-window container-el])
         #(.close ext-window)))
     [])
    win&container))

(defnc Popup [_]
  (let [[win target] @(<-new-window)
        state (<-deref ui-db)
        win-size (<-window-size win)]
    (when target
      (react-dom/createPortal
       (hx/f [Browser {:state state
                       :width (- (:inner-width win-size) 15)}])
       target))))

(defn external-handler [ev]
  (dispatch (edn/read-string ev)))

(defn drawer-toggler []
  (dispatch [:punk.ui.drawer/toggle]))

(defn ^:export start! [node input output opts]
  {:pre [(not (nil? input))
         (not (nil? output))]}
  (.addEventListener
   js/document "keydown"
   (fn [ev]
     (when (and (.-ctrlKey ev) (.-altKey ev) (= "KeyP" (.-code ev)))
       (drawer-toggler))))
  (.unsubscribe ^js input
                external-handler)
  (.subscribe ^js input
              external-handler)
  (f/reg-fx
   ui-frame :emit
   (fn [v]
     (.put ^js output (pr-str v))))
  (let [opts (edn/read-string opts)
        drawer? (get opts :drawer? true)]
    (react-dom/render (hx/f (if drawer?
                              [Popup]
                              [JustBrowser])) node)))
