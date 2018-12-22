(ns punk.workshop.devcards
  (:require [devcards.core :as dc :include-macros true]
            [hx.react :as hx]))

(devcards.core/start-devcard-ui!)

(dc/defcard first-card
  (hx/f [:div "hi"]))
