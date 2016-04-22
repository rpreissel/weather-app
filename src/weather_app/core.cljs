(ns weather-app.core
  (:require [devtools.core :as devtools]
            [weather-app.om :as weo]
            [weather-app.reframe :as wer]))

(enable-console-print!)

; this enables additional features, :custom-formatters is enabled by default
(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)

(defn ^:export run-weather-om []
  (weo/add-root! "app"))

(defn ^:export run-weather-reframe []
  (wer/render-component "app"))
