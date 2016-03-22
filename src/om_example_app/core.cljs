(ns om-example-app.core
  (:require [devtools.core :as devtools]
            [om-example-app.weather :as we]))

(enable-console-print!)

; this enables additional features, :custom-formatters is enabled by default
(devtools/enable-feature! :sanity-hints :dirac)
(devtools/install!)


(we/add-root! "app")