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
                    [:div {:style {:float "right"
                                   :display "flex"}}
                     [:div {:style {:cursor "pointer"
                                    :padding "0 4px"
                                    :margin "0 4px"}} "—"]
                     [:div {:style {:cursor "pointer"
                                    :padding "0 4px"
                                    :margin "0 -4px 0 4px"}} "ｘ"]]]
            :id "punk__inspector__preview"
            :controls [:div {:style {:display "flex"}}
                       [:div {:style {:flex 3
                                      :padding "5px 0" ;;"5px 3px"
                                      :background "white"
                                      :border-right "1px solid #eee"}}]
                       [:div {:style {:flex 9
                                      :padding "5px 8px"}}
                        [:select {:value (str (:id view))
                                  ;; :on-change #(dispatch [:punk.ui.browser/select-current-view
                                  ;;                        (keyword (subs (.. % -target -value) 1))])
                                  }
                         (for [vid (map (comp str :id) views)]
                           [:option {:key vid} vid])]
                        [:div {:style {:float "right"}}
                         [:button {:id "punk__current__back-button"} "Nav"]]]]}
   ;; [:div {:style {:padding "8px"
   ;;                :display "flex"}}
   ;;  [:div {:style {:border-bottom "1px solid #999"
   ;;                 :flex 3}} "key"]
   ;;  [:div {:style {:border-bottom "1px solid #999"
   ;;                 :flex 9}} "value"]]
   [:div {:style {:display "flex" :min-height "100%"
                  ;; :padding-left "8px"
                  ;; :padding-right "8px"
                  }}
    [:div {:style {:flex 3
                   :border-right "1px solid #eee"
                   ;; :padding "8px"
                   }}
     [:div
      (map #(vector
             :div
             {:style (merge {:padding "3px 8px"
                             :margin "3px 0"}
                            (when (= % :asdf)
                              {:background "#eee"}))}
             (str %))
           '(:asdf :jkl))]]
    [:div {:style {:flex 9
                   :padding "8px"}}
     [views/CollView
      {:data (-> current :value :asdf)}]]]])

(defnc Inspector [_]
  [pc/Pane
   {:title [:div
            [:span "Thingy" " :: " " Inspector"]
            [:div {:style {:float "right"
                           :display "flex"}}
             [:div {:style {:cursor "pointer"
                            :padding "0 4px"
                            :margin "0 4px"}} "—"]
             [:div {:style {:cursor "pointer"
                            :padding "0 4px"
                            :margin "0 -4px 0 4px"}} "ｘ"]]]
    :id "punk__current"
    :controls [:div
               [:select {:value (str (:id view))
                         ;; :on-change #(dispatch [:punk.ui.browser/select-current-view
                         ;;                        (keyword (subs (.. % -target -value) 1))])
                         }
                (for [vid (map (comp str :id) views)]
                  [:option {:key vid} vid])]
               [:button {:type "button"
                         :id "punk__current__back-button"
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
