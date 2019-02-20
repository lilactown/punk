(ns punk.workshop.devcards.inspector
  (:require [devcards.core :as dc :include-macros true]
            [punk.ui.components :as punk-ui]
            [punk.ui.inspector :as insp]
            [punk.ui.views :as views]
            [hx.react :as hx :refer [defnc]]
            [hx.hooks :refer [<-state]]
            [clojure.datafy :as d]))

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

(def selected-view {:id :punk.view/map
                    :match map?
                    :view #'views/MapView})

(defnc Container [_]
  (let [state (<-state {:name "MyInspector"
                        :current {:value {:foo {:zxcv {[123] {"bar" :baz}}}
                                          :asdf "jkl"}}
                        :history []
                        :views views
                        :selected-view selected-view
                        :preview? false
                        :preview-views views
                        :preview-selected-view selected-view
                        :preview-selected-key nil
                        :preview-selected-value nil})
        on-select (fn on-select [o k v]
                    (swap! state assoc
                           :preview? true
                           :preview-selected-key k
                           :preview-selected-value v
                           :preview-selected-view selected-view
                           :preview-views views))

        on-back (fn on-back []
                  (swap! state assoc
                         :current (peek (:history @state))
                         :history (pop (:history @state))))

        ;; preview controls
        on-preview-select (fn on-preview-select [k]
                            (swap! state assoc
                                   :preview-selected-key k))
        on-inspector (fn on-inspector []
                       (swap! state assoc
                              :preview? false
                              :preview-selected-key nil
                              :preview-selected-view nil
                              :preview-selected-key nil))
        on-nav (fn on-nav []
                 (swap! state assoc
                        :preview? false
                        :preview-selected-key nil
                        :preview-selected-view nil
                        :preview-selected-key nil
                        :current {:value (:preview-selected-value @state)}
                        :history (conj (:history @state)
                                       (assoc (:current @state)
                                              :nav-key (:preview-selected-key @state)))))]
    (if (:preview? @state)
      [insp/Preview (assoc @state
                           :on-select on-preview-select
                           :on-inspector on-inspector
                           :on-nav on-nav)]
      [insp/Inspector (assoc @state
                             :on-select on-select
                             :on-back on-back)])))

(dc/defcard DemoInspector
  (hx/f [Container]))
