(ns punk.ui.components
  (:require [hx.react :as hx :refer [defnc]]
            [clojure.string :as s]))

(defnc Style [{:keys [children]}]
  [:style {:dangerouslySetInnerHTML #js {:__html (s/join "\n" children)}}])

(defnc Pane [{:keys [title id controls children]}]
  [:div {:id id
         :class "punk__pane__container"}
   [:div {:class "punk__pane__body-container"}
    [:div {:class "punk__pane__titlebar"}
     [:span title]]
    [:div {:class "punk__pane__body"}
     children]]
   (when controls
     [:div {:class "punk__pane__bottom-controls"}
      controls])])

(defnc Table [{:keys [cols data
                      on-entry-click] :as props}]
  (let [key-fn (-> cols first second)]
    [:div (dissoc props :data :cols :on-next :on-entry-click)
     [:div {:style {:display "flex"
                    :border-bottom "1px solid #999"
                    :padding-bottom "3px"
                    :margin-bottom "3px"}}
      (for [[col-name _ display-comp] cols]
        (conj display-comp (name col-name)))]
     (for [d data]
       [:div {:style {:display "flex"
                      :padding "3px 5px"
                      :margin "3px 0"}
              :key (key-fn d)
              :class "item"
              :on-click #(on-entry-click (key-fn d) d)}
        (for [[_ pick display-comp] cols]
          (conj display-comp (prn-str (pick d))))])]))
