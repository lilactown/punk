(ns punk.core
  (:require [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :refer [<-state]]
            ["react-dom" :as react-dom]
            [clojure.datafy :as d]))

(def dbg> (partial js/console.log "punk>"))

(extend-protocol IPrintWithWriter
  js/Symbol
  (-pr-writer [sym writer _]
    (-write writer (str "\"" (.toString sym) "\""))))


;;
;; Data structures
;;

(defprotocol WithIndex
  (with-index [this]))

(extend-protocol WithIndex
  cljs.core/PersistentVector
  (with-index [v] (map-indexed vector v))

  default
  (with-index [x] x))

;;
;; App panes
;;

(defnc View [{:keys [data on-next]}]
  [:div
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
               :on-click #(on-next data key v)}
         [:div {:style {:flex 1}}
          (prn-str key)]
         [:div {:style {:flex 2}}
          (prn-str v)]])]
     (when (not (nil? data))
       [:div {:on-click #(on-next data nil nil)}
        (prn-str data)]))])

(defnc App [_]
  (let [state (<-state {:log [{:foo ["bar" "baz"]
                               :bar {:baz 42}}]
                        :history []
                        :current {:foo ["bar" "baz"]
                                  :bar {:baz 42}}
                        :next {:coll nil
                               :k nil
                               :v nil}})]
    (dbg> @state)
    [:div {:style {:display "flex"
                   :height "100%"
                   :flex-direction "column"}}
     [:div {:style {:flex 1}}
      [View {:data (d/datafy (:current @state))
             :on-next #(swap! state assoc
                              :next
                              {:coll %1
                               :k %2
                               :v %3})}]]
     [:div {:style {:flex 1}}
      [View {:data (d/nav (-> @state :next :coll)
                          (-> @state :next :k)
                          (-> @state :next :v))
             :on-next #(swap! state
                              assoc
                              :history (conj (:history @state)
                                             (:current @state))
                              :current %1
                              :next {:coll nil
                                     :k nil
                                     :v nil})}]]
     [:div [:button {:type "button"
                     :disabled (empty? (:history @state))
                     :on-click #(swap! state assoc
                                       :current (peek (:history @state))
                                       :history (pop (:history @state))
                                       :next {:coll nil :k nil :v nil})}"<"]]]))


(defn start! []
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (dbg> "Creating new container")
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (dbg> "starting")
    (react-dom/render (hx/f [App]) container)))

