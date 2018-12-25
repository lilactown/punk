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

(defnc Table [{:keys [cols data
                      on-entry-click] :as props}]
  (let [key-fn (-> cols first second)]
    [:div (dissoc props :data :cols :on-next :on-entry-click)
     [:div {:style {:display "flex"
                    :border-bottom "1px solid #999"
                    :padding-bottom "3px"
                    :margin-bottom "3px"}}
      (for [[col-name _ style] cols]
        [:div {:style style} (name col-name)])]
     (for [d data]
       [:div {:style {:display "flex"
                      :padding "3px 5px"
                      :margin "3px 0"}
              :key (key-fn d)
              :class "item"
              :on-click #(on-entry-click (key-fn d))}
        (for [[_ pick style] cols]
          [:div {:style style} (prn-str (pick d))])])]))
