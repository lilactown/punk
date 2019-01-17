(ns punk-example.core
  (:require [punk.adapter.web :as punk]
            [goog.object :as gobj]
            [clojure.core.async :as a]))

(punk/start)

(tap> "foo")
