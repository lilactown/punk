(ns punk.workshop
  (:require [punk.core :as punk]
            [punk.ui.core :as punk-ui]))

(punk/remove-taps!)
(punk/add-taps!)

(punk-ui/start!)

#_(tap> {:foo ["bar" ['baz 1 2 3]]})
