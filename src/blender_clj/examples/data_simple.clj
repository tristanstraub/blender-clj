(ns blender-clj.examples.data-simple
  (:require [blender-clj.core :as blender]
            [libpython-clj.python :refer [py..] :as py]))

(def ^:dynamic *bpy*
  nil)

(defmulti bpy-modifier-add
  (fn [defaults config]
    (:modifier/type config)))

(defmethod bpy-modifier-add :array
  [defaults {offset-count :count :keys [use-object-offset offset-object] :as config}]
  (py.. *bpy* -ops -object (modifier_add defaults :type "ARRAY"))
  (let [modifier (py.. *bpy* -context -view_layer -objects -active -modifiers (get "Array"))]
    (when use-object-offset
      (py/set-attr! modifier
                    "use_object_offset"
                    use-object-offset))
    (when offset-object
      (py/set-attr! modifier
                    "offset_object"
                    (py.. *bpy* -data -objects (get (:name offset-object)))))

    (when offset-object
      (py/set-attr! modifier
                    "offset_object"
                    (py.. *bpy* -data -objects (get (:name offset-object)))))
    (when offset-count
      (py/set-attr! modifier "count" offset-count))))

(defmulti bpy-primitive-add
  (fn [defaults config]
    (:primitive config)))

(defmethod bpy-primitive-add :cube
  [defaults {object-name :name :keys [location rotation modifiers] :as config}]
  (py.. *bpy* -ops -mesh
        (primitive_cube_add defaults :location location))
  (let [object (py.. *bpy* -context -view_layer -objects -active)]
    (when object-name
      (py/set-attr! object "name" object-name))
    (when rotation
      (py.. *bpy* -ops -transform (rotate defaults :value (:value rotation)))))

  (run! (partial bpy-modifier-add defaults) modifiers))

(defmethod bpy-primitive-add :cylinder
  [defaults {object-name :name :keys [location modifiers] :as config}]
  (py.. *bpy* -ops -mesh
        (primitive_cylinder_add defaults :location location))
  (when object-name
    (py/set-attr! (py.. *bpy* -context -view_layer -objects -active) "name" object-name))

  (run! (partial bpy-modifier-add defaults) modifiers))

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
                :location  [0 0 0]
                :rotation  {:value 1.0}}
               {:type      :primitive
                :name      "cube-2"
                :primitive :cube
                :location  [10 0 0]}
               {:type      :primitive
                :name      "cylinder-1"
                :primitive :cylinder
                :location  [5 0 0]
                :modifiers [{:type              :modifier
                             :modifier/type     :array
                             :use-object-offset true
                             :offset-object     {:name "cube-1"}
                             :count             5}]}]]

    (delete-all)

    (blender/with-context
      (fn [defaults]
        (binding [*bpy* (py/import-module "bpy")]
          (run! (partial bpy-add defaults) scene))))))

(comment
  (demo)

  )
