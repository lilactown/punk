(ns punk.ui.core
  (:require [hx.react :as hx :refer [defnc]]
            [hx.utils]
            [hx.react.hooks :refer [<-deref]]
            ["react-is" :as react-is]
            ["react-dom" :as react-dom]
            [goog.object :as gobj]
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
 ui-frame :punk.ui/view-entry
 (fn [_ [_ x :as ev]]
   {:punk/dispatch [:punk/view-entry x]}))

(f/reg-event-fx
 ui-frame :punk.ui/nav-to
 (fn [_ [_ coll k v]]
   {:punk/dispatch [:punk/nav-to coll k v]}))

(f/reg-event-fx
 ui-frame :punk.ui/view-next
 (fn [_ [_ x]]
   {:punk/dispatch [:punk/view-next]}))

(f/reg-event-fx
 ui-frame :punk.ui/back
 (fn [_ [_ x]]
   {:punk/dispatch [:punk/history-back]}))


;;
;; App panes
;;

(defnc View [{:keys [data on-next] :as props}]
  (when (not (nil? data))
    [:div (dissoc props :on-next :data)
     (if (coll? data)
       [:<>
        [:div {:style {:display "flex"
                       :border-bottom "1px solid #999"
                       :padding-bottom "3px"
                       :margin-bottom "3px"}}
         [:div {:style {:flex 1}} "key"]
         [:div {:style {:flex 2}} "value"]]
        (for [[key v] (with-index data)]
          [:div {:style {:display "flex"}
                 :key key
                 :class "item"
                 :on-click #(on-next data key v)}
           [:div {:style {:flex 1}}
            (prn-str key)]
           [:div {:style {:flex 2}}
            (prn-str v)]])]
       [:div {:on-click #(on-next data nil nil)}
        (prn-str data)])]))

(defnc Style [{:keys [children]}]
  [:style {:dangerouslySetInnerHTML #js {:__html (s/join "\n" children)}}])

(defnc App [_]
  (let [state (<-deref (.-PUNK_DB js/window))
        ;; dispatch #(f/dispatch (.-PUNK_FRAME js/window) %)
        ]
    [:div {:style {:display "flex"
                   :height "100%"
                   :flex-direction "column"}}
     ;; css
     [Style
      "#current { overflow: scroll }"
      "#current .item { cursor: pointer; padding: 3px; margin: 3px; }"
      "#current .item:hover { background-color: #eee; }"

      "#next { overflow: scroll }"
      "#next { cursor: pointer; padding: 3px; margin: 3px; }"
      "#next:hover { background-color: #eee; }"

      "#log { overflow: scroll }"
      "#log .item { cursor: pointer; padding: 3px 0; margin: 3px 0; }"
      "#log .item:hover { background-color: #eee; }"]
     ;; Next
     [:h3 "Next"]
     [:div {:style {:flex 1
                    :position "relative"
                    :display "flex"
                    :flex-direction "column"}}
      [View {:data (-> state :next :datafied)
             :id "next"
             :on-next #(dispatch [:punk.ui/view-next])}]]
     ;; Current
     [:h3 "Current"]
     [:div {:style {:flex 1
                    :position "relative"
                    :display "flex"
                    :flex-direction "column"}}
      [View {:data (-> state :current :datafied)
             :id "current"
             :on-next #(dispatch [:punk.ui/nav-to %1 %2 %3])}]]
     ;; Controls
     [:div
      [:button {:type "button"
                :style {:width "60px"}
                :disabled (empty? (:history state))
                :on-click #(dispatch [:punk.ui/back])} "<"]]

     ;; Log
     [:h3 "Log"]
     [:div {:style {:flex 1
                    :position "relative"
                    :display "flex"
                    :flex-direction "column"}
            :id "log"}
      (for [entry (:entries state)]
        [:div {:on-click #(dispatch [:punk.ui/view-entry entry])
               :class "item"}
         (prn-str (:datafied entry))])]]))

#_(tap> #js {:asdf "jkl"})
#_(tap> (js/Date.))
#_(tap> (js/RegExp.))

(defn start! []
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (react-dom/render (hx/f [App]) container)))
