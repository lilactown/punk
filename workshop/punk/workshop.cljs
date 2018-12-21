(ns punk.workshop
  (:require [punk.adapter.web]
            [punk.ui.core :as punk-ui]
            [punk.ui.views.frisk]))

(defn start []
  (let [container (or (. js/document getElementById "punk")
                      (let [new-container (. js/document createElement "div")]
                        (. new-container setAttribute "id" "punk")
                        (-> js/document .-body (.appendChild new-container))
                        new-container))]
    (punk-ui/start! container)))

(start)

#_(tap> [{:foo ["bar" ['baz 1 2 3]]} {:asdf ["jkl" 1234 #{1 2 3} ['baz 1 2 3]]}])

#_(tap> #js {:asdf #js ["one" "two"]})

#_(tap> 1)

#_(tap> (js/Error. "foo"))
