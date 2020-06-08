(ns blender-clj.core
  (:require [libpython-clj.require :refer [require-python import-python]]
            [libpython-clj.python :refer [py..] :as py]
            [tech.v2.datatype :as dtype]
            libpython-clj.jna.protocols.object
            [libpython-clj.jna.base :as libpy-base :refer [ensure-pyobj]]
            [tech.jna :as jna])
  (:import [blender_clj DirectMapped]))

(def bpy-so-path
  (or (System/getenv "BPY_BIN_PATH")
      "vendor/blender-git/build_linux/bin"))

(defn ui-main
  []
  (let [library (jna/load-library libpy-base/*python-library*)]
    (com.sun.jna.Native/register DirectMapped library)

    (let [bpy         (py/import-module "bpy")
          _bpy        (py/import-module "_bpy")
          ui-main-ptr (ensure-pyobj (py.. _bpy (ui_main)))
          func        (com.sun.jna.Function/getFunction (DirectMapped/PyCapsule_GetPointer ui-main-ptr "ui_main"))]
      (.invoke func (make-array Object 0)))))

(def gui-future
  (delay (let [f (future (let [sys (py/import-module "sys")]
                           (py.. sys -path (append bpy-so-path))
                           (ui-main)))]
           (Thread/sleep 5000)
           f)))

(defn ensure-gui
  []
  @gui-future)

(defn get-defaults
  []
  (let [bpy    (py/import-module "bpy")
        window (first (seq (py.. bpy -context -window_manager -windows)))]
    {"window" window
     "screen" (py.. window -screen)}))

(defn with-context
  [f]
  (let [bpy (py/import-module "bpy")]
    (py.. bpy -app -timers
          (register (fn []
                      (try
                        (f (get-defaults))
                        (catch Exception e
                          (println e)))
                      nil)))))
