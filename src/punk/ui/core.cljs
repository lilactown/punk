(ns punk.ui.core
  (:require [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :refer [<-deref]]
            ["react-dom" :as react-dom]
            ["react-grid-layout" :as GridLayout]
            [clojure.string :as s]
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
;; UI Events
;;

(def ui-frame (f/create-frame))

(def dispatch #(f/dispatch ui-frame %))

;; dispatch to the app
(f/reg-fx
 ui-frame :punk/dispatch
 (fn punk-dispatch-fx [v]
   (f/dispatch (.-PUNK_FRAME js/window) v)))

(f/reg-event-fx
 ui-frame :punk.ui.browser/view-entry
 (fn [_ [_ x :as ev]]
   {:punk/dispatch [:punk.browser/view-entry x]}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/nav-to
 (fn [_ [_ coll k v]]
   {:punk/dispatch [:punk.browser/nav-to coll k v]}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/view-next
 (fn [_ [_ x]]
   {:punk/dispatch [:punk.browser/view-next]}))

(f/reg-event-fx
 ui-frame :punk.ui.browser/back
 (fn [_ [_ x]]
   {:punk/dispatch [:punk.browser/history-back]}))

;;
;; Data views
;;

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

(defn match-view [views data]
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

(defnc Browser [_]
  (let [state (<-deref (.-PUNK_DB js/window))]
    [:div {:style {:height "100%"} #_{:position "absolute"
                   :top 0 :left 0}}
     [Style
      "#current { overflow: auto }"
      "#current .item { cursor: pointer; padding: 3px; margin: 3px; }"
      "#current .item:hover { background-color: #eee; }"

      "#next { overflow: auto }"
      "#next { cursor: pointer; padding: 3px; margin: 3px; }"
      "#next:hover { background-color: #eee; }"
      "#next.nohover { cursor: initial; }"
      "#next.nohover:hover { background-color: initial; }"

      "#log { overflow: auto }"
      "#log .item { cursor: pointer; padding: 3px 0; margin: 3px 0; }"
      "#log .item:hover { background-color: #eee; }"]
     [GridLayout {:class "layout" :layout layout :cols 12
                  :rowHeight 30
                  :width 800}
      ;; Next
      [:div {:key "next"}
       [:h3 "Next"]
       [:div {:style {:display "flex"
                      :flex-direction "column"}}
        [(match-view views (-> state :next :datafied))
         {:data (-> state :next :datafied)
          :id "next"
          :on-next #(dispatch [:punk.ui.browser/view-next])}]]]
      ;; Current
      [:div {:key "current"}
       [:h3 "Current"]
       [:div {:style {:display "flex"
                      :flex-direction "column"}}
        [(match-view views (-> state :current :datafied))
         {:data (-> state :current :datafied)
          :id "current"
          :on-next #(dispatch [:punk.ui.browser/nav-to %1 %2 %3])}]]
       ;; Controls
       [:div
        [:button {:type "button"
                  :style {:width "60px"}
                  :disabled (empty? (:history state))
                  :on-click #(dispatch [:punk.ui.browser/back])} "<"]]];; Entrie
      [:div {:key "entries"}
       [:h3 "Entries"]
       [:div {:style {:display "flex"
                      :flex-direction "column"}
              :id "log"}
        (for [[idx entry] (reverse (map-indexed vector (:entries state)))]
          [:div {:on-click #(dispatch [:punk.ui.browser/view-entry entry])
                 :class "item"}
           idx " " (prn-str (:datafied entry))])]]]]))

(defn start! []
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (react-dom/render (hx/f [Browser]) container)))
