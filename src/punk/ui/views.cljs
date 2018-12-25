(ns punk.ui.views
  (:require [hx.react :as hx :refer [defnc]]
            [punk.ui.components :as pc]))

(defnc MapView [{:keys [data nav] :as props}]
  [pc/Table (merge props
                   {:cols [[:key first {:flex 1}]
                           [:value second {:flex 3}]]
                    :on-entry-click (fn [key] (nav data key (get data key)))
                    :data data})])

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

