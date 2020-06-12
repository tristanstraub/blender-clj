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
    (py/import-module "bpy")

    (let [library (jna/load-library (str bpy-so-path "/bpy.so"))]
      (com.sun.jna.Native/register DirectMapped library)

      (DirectMapped/WM_python_main))))

(defn get-defaults
  []
  (let [bpy    (py/import-module "bpy")
        window (first (seq (py.. bpy -context -window_manager -windows)))]
    {"window" window
     "screen" (py.. window -screen)}))

(def ^:dynamic *in-timer?*
  false)

(defn with-context-transaction
  [state f]
  (let [bpy      (py/import-module "bpy")
        p        (promise)
        timer-fn (fn []
                   (try
                     (deliver p (f (get-defaults)))
                     (catch Exception e
                       (deliver p e)))
                   nil)]
    (if *in-timer?*
      (timer-fn)
      (py.. bpy -app -timers (register (fn []
                                         (binding [*in-timer?* true]
                                           (timer-fn))))))
    (let [result @p]
      (if (instance? Exception result)
        (throw result)
        result))))

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
  (send with-context-agent (skip-exceptions with-context-transaction) f))
