(ns punk.ui.inspector
  (:require [hx.react :as hx :refer [defnc]]
            [hx.hooks :refer [<-state]]
            [punk.ui.components :as pc]
            [punk.ui.views :as views]
            [clojure.string :as s]))

(def views [{:id :punk.view/nil
             :match nil?
             :view nil}

            {:id :punk.view/map
             :match map?
             :view #'views/MapView}

            {:id :punk.view/set
             :match set?
             :view #'views/SetView}

            {:id :punk.view/coll
             :match (every-pred
                     coll?
                     (comp not map?))
             :view #'views/CollView}

            {:id :punk.view/edn
             :match any?
             :view #'views/EdnView}])

(def view {:id :punk.view/map
           :match map?
           :view #'views/MapView})

(def history [{:nav-key 0}
              {:nav-key :cdk/component}
              {:nav-key [123]}])

(def current {:value {:asdf ['jkl]
                      :foo {:bar #{:baz/yuiop}}}})

(defnc Preview [_]
  [pc/Pane {:title [:div
                    [:span "Thingy" " :: "
                     [:a {:href ""
                          :on-click #(.preventDefault %)} "Inspector"] " > " "Preview"]
                    [:div {:class "punk__pane__top-controls"}
                     [:div "—"]
                     [:div "ｘ"]]]
            :class "punk__inspector__preview"
            :controls [:div
                       [:div {:class "punk__pane__bottom-controls__spacer"}]
                       [:div {:class "punk__pane__bottom-controls__controls"}
                        [:select {:value (str (:id view))
                                  ;; :on-change #(dispatch [:punk.ui.browser/select-current-view
                                  ;;                        (keyword (subs (.. % -target -value) 1))])
                                  }
                         (for [vid (map (comp str :id) views)]
                           [:option {:key vid} vid])]
                        [:div {:class "punk__pane__bottom-controls__right-align"}
                         [:button {:class ["punk__pane__bottom-button"
                                           "punk__inspector__nav-button"]} "Nav"]]]]}
   ;; [:div {:style {:padding "8px"
   ;;                :display "flex"}}
   ;;  [:div {:style {:border-bottom "1px solid #999"
   ;;                 :flex 3}} "key"]
   ;;  [:div {:style {:border-bottom "1px solid #999"
   ;;                 :flex 9}} "value"]]
   [:div {:class "punk__inspector__preview__container"}
    [:div {:class "punk__inspector__preview__key-list"}
     [:div
      (map #(vector
             :div
             {:class (if (= % :asdf)
                       ["punk__inspector__preview__key-list__item"
                        "punk__inspector__preview__key-list__item__selected"]
                       "punk__inspector__preview__key-list__item")}
             (str %))
           '(:asdf :jkl))]]
    [:div {:class "punk__inspector__preview__view"}
     [views/CollView
      {:data (-> current :value :asdf)}]]]])

(defnc Inspector [_]
  [pc/Pane
   {:title [:div
            [:span "Thingy" " :: " " Inspector"]
            [:div {:class "punk__pane__top-controls"}
             [:div "—"]
             [:div "ｘ"]]]
    :class "punk__inspector"
    :controls [:div
               [:select {:value (str (:id view))
                         ;; :on-change #(dispatch [:punk.ui.browser/select-current-view
                         ;;                        (keyword (subs (.. % -target -value) 1))])
                         }
                (for [vid (map (comp str :id) views)]
                  [:option {:key vid} vid])]
               [:button {:type "button"
                         :class ["punk__pane__bottom-button"
                                 "punk__inspector__back-button"]
                         :disabled (empty? history)
                         ;; :on-click #(dispatch [:punk.ui.browser/history-back])
                         } "<"]
               [pc/Breadcrumbs
                {:items (map :nav-key history)
                 ;; :on-click #(dispatch [:punk.ui.browser/history-nth %])
                 }]]}
   [(:view view)
    {:data (-> current :value)
     ;; :nav  #(dispatch [:punk.ui.browser/nav-to
     ;;             (-> current :idx) %2 %3])
     }]])
