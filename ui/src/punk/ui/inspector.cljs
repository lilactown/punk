(ns punk.ui.inspector
  (:require [hx.react :as hx :refer [defnc]]
            [hx.hooks :refer [<-state]]
            [punk.ui.components :as pc]
            [clojure.string :as s]))

(defnc Preview [{:keys [current name preview-selected-key
                        preview-views preview-selected-view
                        on-minimize on-close
                        on-inspector on-view-select
                        on-select on-nav]}]
  [pc/Pane {:title [:div
                    [:span "[" name "] "
                     [:a {:href ""
                          :on-click #(do (.preventDefault %)
                                         (on-inspector))}
                      "Inspector"]
                     " > " "Preview"]
                    [pc/TopControls {:on-close on-close :on-minimize on-minimize}]]
            :class "punk__inspector__preview"
            :controls [:div
                       [:div {:class "punk__pane__bottom-controls__spacer"}]
                       [:div {:class "punk__pane__bottom-controls__controls"}
                        [:select {:value (str (:id preview-selected-view))
                                  ;; :on-change #(dispatch [:punk.ui.browser/select-current-view
                                  ;;                        (keyword (subs (.. % -target -value) 1))])
                                  :on-change on-view-select}
                         (for [vid (map (comp str :id) preview-views)]
                           [:option {:key vid} vid])]
                        [:div {:class "punk__pane__bottom-controls__right-align"}
                         [:button {:class ["punk__pane__bottom-button"
                                           "punk__inspector__nav-button"]
                                   :on-click on-nav} "Nav"]]]]}
   [:div {:class "punk__inspector__preview__container"}
    [:div {:class "punk__inspector__preview__key-list"}
     [:div
      (map #(vector
             :div
             {:class (if (= % preview-selected-key)
                       ["punk__inspector__preview__key-list__item"
                        "punk__inspector__preview__key-list__item__selected"]
                       "punk__inspector__preview__key-list__item")
              :on-click (when (not= % preview-selected-key)
                          (fn [] (on-select %)))}
             (str %))
           (keys (:value current)))]]
    [:div {:class "punk__inspector__preview__view"}
     [(:view preview-selected-view)
      {:data (-> current :value (get preview-selected-key))
       :nav #()}]]]])

(defnc Inspector [{:keys [current history name
                          views selected-view
                          on-minimize on-close
                          on-select on-history-select
                          on-back on-view-select]}]
  [pc/Pane
   {:title [:div
            [:span "[" name "] " " Inspector"]
            [pc/TopControls {:on-close on-close :on-minimize on-minimize}]]
    :class "punk__inspector"
    :controls [:div
               [:select {:value (str (:id selected-view))
                         ;; :on-change #(dispatch [:punk.ui.browser/select-current-view
                         ;;                        (keyword (subs (.. % -target -value) 1))])
                         :on-change on-view-select}
                (for [vid (map (comp str :id) views)]
                  [:option {:key vid} vid])]
               [:button {:type "button"
                         :class ["punk__pane__bottom-button"
                                 "punk__inspector__back-button"]
                         :disabled (empty? history)
                         ;; :on-click #(dispatch [:punk.ui.browser/history-back])
                         :on-click on-back} "<"]
               [pc/Breadcrumbs
                {:items (map :nav-key history)
                 ;; :on-click #(dispatch [:punk.ui.browser/history-nth %])
                 :on-click on-history-select}]]}
   [(:view selected-view)
    {:data (-> current :value)
     ;; :nav  #(dispatch [:punk.ui.browser/nav-to
     ;;             (-> current :idx) %2 %3])
     :nav on-select}]])
