(ns blender-clj.core
  (:require [libpython-clj.require :refer [require-python import-python]]
            [libpython-clj.python :refer [py..] :as py]
            [tech.v2.datatype :as dtype]
            libpython-clj.jna.protocols.object
            [libpython-clj.jna.base :as libpy-base :refer [ensure-pyobj]]
            [tech.jna :as jna]
            [clojure.java.io :as io])
  (:import [blender_clj DirectMapped]))

(def bpy-so-path
  (or (System/getenv "BPY_BIN_PATH")
      (.getAbsolutePath (io/file (io/resource "blender-git/build_linux/bin")))))

(defn ui-main
  []
  (let [sys     (doto (py/import-module "sys")
                  (py.. -path (append bpy-so-path)))
        library (jna/load-library libpy-base/*python-library*)]
    (com.sun.jna.Native/register DirectMapped library)

    (let [bpy         (py/import-module "bpy")
          _bpy        (py/import-module "_bpy")
          ui-main-ptr (ensure-pyobj (py.. _bpy (ui_main)))
          func        (com.sun.jna.Function/getFunction (DirectMapped/PyCapsule_GetPointer ui-main-ptr "ui_main"))]
      (.invoke func (make-array Object 0)))))

(defn get-defaults
  []
  (let [bpy    (py/import-module "bpy")
        window (first (seq (py.. bpy -context -window_manager -windows)))]
    {"window" window
     "screen" (py.. window -screen)}))

(def ^:dynamic *in-timer?*
  false)

(defn with-context-transaction
  [p _ f]
  (let [bpy      (py/import-module "bpy")
        p        (promise)
        timer-fn (fn []
                   (try
                     (deliver p (f (get-defaults)))
                     (catch Exception e
                       (deliver p e)))
                   nil)]
    (py.. bpy -app -timers (register (fn []
                                       (binding [*in-timer?* true]
                                         (timer-fn)))))
    p))

(defn resolve-promise
  [p]
  (let [result @p]
    (if (instance? Exception result)
      (throw result)
      result)))

(defonce with-context-agent
  (agent nil))

(defn skip-exceptions
  [f]
  (fn [state & args]
    (try
      (apply f state args)
      (catch Exception e
        (println e)
        state))))

(defn with-context
  [f]
  (if *in-timer?*
    (f (get-defaults))
    (let [p (promise)]
      (send with-context-agent (skip-exceptions (partial with-context-transaction p)) f)
      (resolve-promise p))))
