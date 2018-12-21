(ns punk.ui.views.frisk
  (:require [hx-frisk.view :as frisk]
            [hx.react :as hx :refer [defnc]]
            [hx.react.hooks :refer [<-state]]
            [punk.ui.core :as punk-ui]))

(defnc View [{:keys [data on-click]}]
  (let [state (<-state {:data-frisk nil})]
    [frisk/Root {:data data
                 :state-atom state
                 :id "punk-ui"}]))

(punk-ui/unregister-view! :punk.view/frisk)

(punk-ui/register-view!
 {:id :punk.view/frisk
  :match any?
  :view View})
