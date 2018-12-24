(ns punk.workshop.devcards
  (:require [devcards.core :as dc :include-macros true]
            [punk.ui.components :as punk-ui]
            [hx.react :as hx]))

(devcards.core/start-devcard-ui!)

(dc/defcard pane
  (hx/f [:div {:style {:position "relative"
                       :height "200px"} }
         [punk-ui/Pane {:title "Title" :id "pane-1"}
          [:div "Children"]]]))

(dc/defcard pane-with-long-content
  (hx/f [:div {:style {:height "200px"}}
         [punk-ui/Pane {:title "Title" :id "pane-1"}
          (for [n (range 20)]
            [:div "Child " n])]]))

(hx/defnc Controls [_]
  [:button " < "])

(dc/defcard pane-with-controls
  (hx/f [:div {:style {:height "200px"}}
         [punk-ui/Pane {:title "Title" :id "pane-1"
                        :controls [Controls]}
          (for [n (range 20)]
            [:div "Child " n])]]))
