(ns blender-clj.nrepl
  (:require [nrepl.server :refer [start-server stop-server]]
            [blender-clj.core :refer [ui-main]]))

(println "NREPL started on port:" (:port (start-server)))
(ui-main)
