(ns punk.ui.components
  (:require [hx.react :as hx :refer [defnc]]
            [clojure.string :as s]))

(defnc Style [{:keys [children]}]
  [:style {:dangerouslySetInnerHTML #js {:__html (s/join "\n" children)}}])

(defnc Pane [{:keys [title id controls children]}]
  [:div {:id id
         :style {:border "1px solid #ddd"
                 :box-shadow "2px 2px 1px 1px #eee"
                 :height "100%"
                 :background "white"
                 :position "relative"
                 :display "flex"
                 :flex-direction "column"}}
   [:div {:style {:overflow "auto"
                  :flex 1}}
    [:div {:style {:background "#eee"
                   :padding "8px"
                   :position "sticky"
                   :top 0
                   :z-index "2"
                   :cursor "move"}
           :class "titlebar"}
     [:span {:style {:font-size "1.17em"
                     :font-weight "500"}}
      title]]
    [:div {:style {:padding "8px"}}
     children]]
   (when controls
     [:div {:style {:padding "3px 8px"
                    :background "#eee"}}
      controls])])
