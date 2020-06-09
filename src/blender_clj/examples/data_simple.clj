(ns blender-clj.examples.data-simple
  (:require [blender-clj.core :as blender]
            [libpython-clj.python :refer [py..] :as py]))

(def ^:dynamic *bpy*
  nil)

(defmulti bpy-primitive-add
  (fn [defaults config]
    (:primitive config)))

(defmethod bpy-primitive-add :cube
  [defaults {object-name :name :keys [location] :as config}]
  (py.. *bpy* -ops -mesh
        (primitive_cube_add defaults :location location))
  (when object-name
    (py/set-attr! (py.. *bpy* -context -view_layer -objects -active) "name" object-name)))

(defmethod bpy-primitive-add :cylinder
  [defaults {object-name :name :keys [location] :as config}]
  (py.. *bpy* -ops -mesh
        (primitive_cylinder_add defaults :location location))
  (when object-name
    (py/set-attr! (py.. *bpy* -context -view_layer -objects -active) "name" object-name)))

(defmulti bpy-add
  (fn [defaults config]
    (:type config)))

(defmethod bpy-add :primitive
  [defaults config]
  (bpy-primitive-add defaults config))

(defn delete-all
  []
  (blender/with-context
    (fn [defaults]
      (binding [*bpy* (py/import-module "bpy")]
        (py.. *bpy* -ops -object (select_all defaults :action "SELECT"))
        (py.. *bpy* -ops -object (delete defaults))))))

(defn demo
  []
  (blender/ensure-gui)

  (let [scene [{:type      :primitive
                :name      "cube-1"
                :primitive :cube
                :location  [0 0 0]}
               {:type      :primitive
                :name      "cube-2"
                :primitive :cube
                :location  [10 0 0]}
               {:type      :primitive
                :name      "cylinder-1"
                :primitive :cylinder
                :location  [5 0 0]}]]

    (delete-all)

    (blender/with-context
      (fn [defaults]
        (binding [*bpy* (py/import-module "bpy")]
          (run! (partial bpy-add defaults) scene))))))

(comment
  (demo)

  )
