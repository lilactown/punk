(ns punk.workshop.devcards
  (:require [devcards.core :as dc :include-macros true]
            [punk.ui.components :as punk-ui]
            [punk.ui.inspector :as insp]
            [punk.ui.views :as views]
            [hx.react :as hx]))

(devcards.core/start-devcard-ui!)

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

(dc/defcard Plain-Inspector
  (hx/f [:div {:style {:height "200px"}}
         [insp/Inspector
          {:views views
           :selected-view view
           :value (:value current)
           :history history}]]))

(dc/defcard Preview-Inspector
  (hx/f [:div {:style {:height "200px"}}
         [insp/Preview
          {:views views
           :selected-view {:id :punk.view/coll
                           :match (every-pred
                                   coll?
                                   (comp not map?))
                           :view #'views/CollView}
           :value (:value current)
           :selected-key :asdf}]]))

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

(dc/defcard table-coll
  (hx/f [punk-ui/Table {:cols [[:idx first [:div {:style {:flex 1}}]]
                               [:value second [:div {:style {:flex 3}}]]]
                        :data (map-indexed vector [1 2 3 4])}]))

(dc/defcard table-map
  (hx/f [punk-ui/Table {:cols [[:key first [:div {:style {:flex 1}}]]
                               [:value second [:div {:style {:flex 3}}]]]
                        :data {:foo "bar" :baz #{1 2 3}}}]))

(dc/defcard table-multiple-things
  (hx/f [punk-ui/Table {:cols [[:key first [:div {:style {:flex 1}}]]
                               [:value second [:div {:style {:flex 3}}]]
                               [:meta (comp meta second) [:div {:style {:flex 3}}]]]
                        :data {:foo (with-meta {:asdf "jkl"} {:punk/tag 'cljs.core/Map})
                               :baz (with-meta #{1 2 3} {:punk/tag 'cljs.core/Set})}}]))
