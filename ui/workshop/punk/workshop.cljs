(ns punk.workshop
  (:require [punk.ui.remote :as punk]
            [punk.ui.views.frisk]))

;; (def app ""
;;   "<div style=\"text-align: center; padding: 10px;\">foo bar</div>")

;; (set! (-> js/document
;;           (.-body)
;;           (.-innerHTML)) app)

(defn ^{:export true
        :dev/after-load true}
  start []
  (punk/start!))

(tap> [{:foo ["bar" ['baz 1 2 3]]} {:asdf ["jkl" 1234 #{1 2 3} ['baz 1 2 3]]}])

#_(tap> (with-meta {:foo [1 2 3]} {:asdf "jkl"}))

#_(tap> (with-meta (range 10) {:punk/form '(range 10)}))

#_(tap> #js {:asdf #js ["one" "two"]})

#_(tap> 1)

#_(tap> (js/Error. "foo"))

#_(tap> "Hi chris!")

#_(tap> ['foo 'bar 'baz 1234])
