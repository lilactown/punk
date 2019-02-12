(ns punk.ui.inspector
  (:require [hx.react :as hx :refer [defnc]]
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
              {:nav-key [123]}])

(def current {:value {:asdf ['jkl]
                      :foo {:bar #{:baz/yuiop}}}})

(defnc Inspector [_]
  [pc/Pane
   {:title "Inspector"
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
