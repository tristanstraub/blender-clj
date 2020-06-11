(ns blender-clj.examples.cube
  (:require [blender-clj.core :as blender]
            [libpython-clj.python :refer [py..] :as py]))

(defn demo
  []
  (let [bpy (py/import-module "bpy")]
    (blender/with-context
      (fn [defaults]
        (dotimes [_ 10]
          (py.. bpy -ops -mesh (primitive_cube_add defaults :size 3 :location (vec (repeatedly 3 #(- (rand-int 20) 10))))))))))

(comment
  (demo)

  )
