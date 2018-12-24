(ns punk.ui.views
  (:require [hx.react :as hx :refer [defnc]]
            [punk.ui.components :as pc]))

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
  [pc/Table (merge props
                   {:cols [[:idx first {:flex 1}]
                           [:value (comp prn-str second) {:flex 3}]]
                    :on-entry-click (fn [key] (on-next data key (get data key)))
                    :data (map-indexed vector data)})])

(defnc SetView [{:keys [data on-next] :as props}]
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
        (prn-str v)]])]])

(defnc EdnView [{:keys [data on-next] :as props}]
  [:div [:code (prn-str data)]])

