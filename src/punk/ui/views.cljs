(ns punk.ui.views
  (:require [hx.react :as hx :refer [defnc]]
            [punk.ui.components :as pc]))

(defnc MapView [{:keys [data nav] :as props}]
  [:div (dissoc props :nav :data)
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
             :on-click #(nav data key v)}
       [:div {:style {:flex 1}}
        (prn-str key)]
       [:div {:style {:flex 2}}
        (prn-str v)]])]])

(defnc CollView [{:keys [data nav] :as props}]
  [pc/Table (merge props
                   {:cols [[:idx first {:flex 1}]
                           [:value second {:flex 3}]]
                    :on-entry-click (fn [key] (nav data key (get data key)))
                    :data (map-indexed vector data)})])

(defnc SetView [{:keys [data nav] :as props}]
  [pc/Table (merge props
                   {:cols [[:value identity]]
                    :on-entry-click (fn [v] (nav data nil v))
                    :data data})])

(defnc EdnView [{:keys [data nav] :as props}]
  [:div [:code (prn-str data)]])

