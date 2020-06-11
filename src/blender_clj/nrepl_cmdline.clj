(ns blender-clj.nrepl-cmdline
  (:require [blender-clj.core :refer [ui-main]]
            [nrepl.cmdline]))

(defn -main
  [& args]
  ;; Run nrepl.cmdline so that --middleware options work
  (future (apply nrepl.cmdline/-main args))
  (ui-main))
