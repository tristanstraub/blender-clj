#!/bin/bash

# https://wiki.blender.org/wiki/Building_Blender/Linux/Fedora

function get-blender {
    mkdir -p vendor/blender-git
    cd vendor/blender-git
    git clone https://git.blender.org/blender.git
}

function get-libraries {
    mkdir -p vendor/blender-git/lib
    cd vendor/blender-git/lib
    svn checkout https://svn.blender.org/svnroot/bf-blender/trunk/lib/linux_centos7_x86_64
}

function build-module {
    cd vendor/blender-git/blender
    make update
    patch -p1 < ../../../backgroundgui.patch
    BUILD_CMAKE_ARGS="-DWITH_MEM_JEMALLOC=OFF -DWITH_PYTHON_INSTALL=OFF -DWITH_AUDASPACE=OFF -DWITH_PYTHON_MODULE=ON" make
}

function build-jna-mapper {
    mkdir -p lib
    (cd lib; curl -L https://github.com/java-native-access/jna/archive/5.5.0.tar.gz | tar -xvzf -)

    javac -cp ./lib/jna-5.5.0/dist/jna.jar src/blender_clj/DirectMapped.java
}

function execute {
    BPY_BIN_PATH=vendor/blender-git/build_linux/bin clj -e "(require 'blender-clj.core) (blender-clj.core/gui)" -r
}

$@
