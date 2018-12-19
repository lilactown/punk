(ns punk.workshop
  (:require [punk.core :as punk]
            [punk.ui.core :as punk-ui]))

(punk/add-taps!)

(punk-ui/start!)

#_(tap> [{:foo ["bar" ['baz 1 2 3]]} {:asdf ["jkl" 1234 #{1 2 3} ['baz 1 2 3]]}])

#_(tap> #js {:asdf #js ["one" "two"]})
