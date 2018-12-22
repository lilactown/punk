(ns punk.ui.core
  (:require [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :refer [<-deref]]
            ["react-dom" :as react-dom]
            ["react-grid-layout" :as GridLayout]
            [goog.object :as gobj]
            [clojure.string :as s]
            [clojure.core.async :as a]
            [frame.core :as f]
            [punk.ui.views :as views]))

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
                      :next/loading false
                      :next nil
                      :views [{:id :punk.view/nil
                               :match nil?
                               :view nil}

                              {:id :punk.view/map
                               :match map?
                               :view views/MapView}

                              {:id :punk.view/set
                               :match set?
                               :view views/SetView}

                              {:id :punk.view/coll
                               :match (every-pred
                                       coll?
                                       (comp not map?))
                               :view views/CollView}

                              {:id :punk.view/edn
                               :match any?
                               :view views/EdnView}]
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
 ui-frame :punk.ui.browser/view-entry
 [debug-db debug-event]
 (fn [{:keys [db]} [_ x]]
   {:db (assoc db
               :current x
               :next nil
               :next/loading false
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
                   :next nil
                   :view/selected nil))}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/nav-to
 [#_debug-db debug-event]
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
 [debug-db]
 (fn [{:keys [db]} [_ v]]
   {:db (update db :views conj v)}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/unregister-view
 []
 (fn [{:keys [db]} [_ id]]
   ;; filterv here is important to preserve order
   (let [views' (filterv #(not= id (:id %)) (:views db))]
     {:db (assoc db :views views')})))

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
 [#_debug-db debug-event]
 (fn [cofx [_ idx x]]
   {:db (update (:db cofx) :entries conj (assoc x :idx idx))}))

(f/reg-event-fx
 ui-frame :nav
 [debug-event debug-db]
 (fn [{:keys [db]} [_ idx x]]
   {:db (assoc db
               :next/loading false
               :next x
               :view/selected nil)}))


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

(defnc Pane [{:keys [title id children]}]
  [:div {:id id}
   [:h3 title]
   children])

(defnc Browser [_]
  (let [state (<-deref ui-db)
        next-views (-> (:views state)
                       (match-views (-> state :next :value)))

        next-view (if (:view/selected state)
                    (first (filter #(= (:id %) (:view/selected state)) next-views))
                    (first next-views))
        current-view (-> (:views state)
                         (match-views (-> state :current :value))
                         (first))]
    (js/console.log next-views)
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
      [:div {:key "next"}
       [Pane {:id "next-grid" :title "Next"}
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
           :on-next #(dispatch [:punk.ui.browser/view-next])}]]]]
      ;; Current
      [:div {:key "current" :id "current-grid"}
       [:h3 "Current"]
       [:div {:style {:display "flex"
                      :flex-direction "column"}}
        [(:view current-view)
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

(defn ^:export start! [node]
    (a/go-loop []
      (let [ev (a/<! (gobj/get js/window "PUNK_IN_STREAM"))]
        (println ev)
        (dispatch ev)
        (recur)))
    (f/reg-fx
     ui-frame :emit
     (fn [v]
       (a/put! (gobj/get js/window "PUNK_OUT_STREAM") v)))
    (react-dom/render (hx/f [Browser]) node))

#_(dispatch [:punk.ui.browser/select-view-type :punk.view/frisk])
