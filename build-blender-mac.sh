#!/usr/bin/env bash
set -euo pipefail

# https://wiki.blender.org/wiki/Building_Blender/Mac

# Dependencies:
#   1. install Xcode Development Tools
#   2. install python 3.7 from https://www.python.org/downloads/release/python-377
#      Under my 10.15.5:
#      - I used their macOS installer, it should install itself to /Library/Frameworks/Python.framework/Versions/3.7
#      - had to do `ln -s /Library/Frameworks/Python.framework/Versions/3.7/lib/python3.7/config-3.7m-darwin /Library/Frameworks/Python.framework/Versions/3.7/lib/python3.7/config-3.7m`
#   3. install homebrew => https://brew.sh
#   4. run/see install_deps below
#
# Getting Blender:
#   1. export BLENDER_GIT_DIR, e.g. set it to "$ROOT_DIR/vendor/blender-git"
#   2. git clone https://git.blender.org/blender.git "$BLENDER_GIT_DIR"
#   3. note that Blender's make process will create lib and build_<platform> directory in parent of "$BLENDER_GIT_DIR"
#      effectively BLENDER_LIB_DIR="$BLENDER_GIT_DIR/../lib"
#      effectively BLENDER_BUILD_DIR="$BLENDER_GIT_DIR/../build_darwin"

set -x

cd "$(dirname "${BASH_SOURCE[0]}")"

ROOT_DIR="$(pwd)"
OUR_LIB_DIR="$ROOT_DIR/lib"
BLENDER_GIT_DIR=${BLENDER_GIT_DIR:-./vendor/blender-git}
BLENDER_LIB_DIR="$BLENDER_GIT_DIR/../lib"
BLENDER_BUILD_DIR="$BLENDER_GIT_DIR/../build_darwin"
JNA_VERSION=${JNA_VERSION:-"5.5.0"}

ensure_lib_dir() {
  if [[ ! -d "$OUR_LIB_DIR" ]]; then
    mkdir -p "$OUR_LIB_DIR"
  fi
}

install_deps() {
  brew install git svn cmake
  # and maybe more... haven't tested this on a clean system
}

build_module() {
  cd "$BLENDER_GIT_DIR"

  # remove previously applied patches and intermediate files
  git clean -fd
  git reset --hard HEAD

  git submodule update --init --recursive

  make update
  patch -p1 <"$ROOT_DIR/backgroundgui.patch"
  CMAKE_ARGS=(
    # TODO: I had to disable openMP support under macOS, it looks like Blender's make does not properly link to
    #       $BLENDER_LIB_DIR/darwin/openmp/lib/libomp.dylib
    #       I didn't figure out how to fix it, so I simply disable it for now
    "-DWITH_OPENMP=OFF"
    "-DWITH_MEM_JEMALLOC=OFF"
    "-DWITH_PYTHON_INSTALL=OFF"
    "-DWITH_AUDASPACE=OFF"
    "-DWITH_PYTHON_MODULE=ON"
  )
  export BUILD_CMAKE_ARGS="${CMAKE_ARGS[*]}"
  make
}

build_jna_mapper() {
  ensure_lib_dir
  TMP_JNA_LIB=/tmp/jna.tar.gz
  if [[ ! -f "$TMP_JNA_LIB" ]]; then
    curl -L "https://github.com/java-native-access/jna/archive/$JNA_VERSION.tar.gz" -o "$TMP_JNA_LIB"
  fi
  tar -C "$OUR_LIB_DIR" -xvzf "$TMP_JNA_LIB"
  javac -cp "$OUR_LIB_DIR/jna-$JNA_VERSION/dist/jna.jar" "$ROOT_DIR/src/blender_clj/DirectMapped.java"
}

repl() {
  export BPY_BIN_PATH="$BLENDER_BUILD_DIR/bin"
  exec clj -r
}

if [[ $# -eq 0 ]]; then
  build_module
  build_jna_mapper
else
  "$1"
fi
