(ns blender-clj.examples.add-cube
  (:require [blender-clj.core :as blender]
            [libpython-clj.python :refer [py..] :as py]))

(defn cube
  []
  (let [bpy (py/import-module "bpy")]
    (blender/with-context
      (fn [ctx]
        (py.. bpy -ops -mesh (primitive_cube_add ctx))
        nil))))
