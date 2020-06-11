#!/bin/bash
set -euxo pipefail

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

function server {
    BPY_BIN_PATH=vendor/blender-git/build_linux/bin clj -A:nREPL | grep -o "nrepl://[^:]*:.*" | cut -d ":" -f 3 > .nrepl-port
}

function repl {
    rm -f .nrepl-port
    (server &)
    while [ ! -e .nrepl-port ] || [ -z "$(cat .nrepl-port)" ]; do
        sleep 1
    done
    BPY_BIN_PATH=vendor/blender-git/build_linux/bin clj -R:nREPL -m nrepl.cmdline -c -p $(cat .nrepl-port)

}

if [ -z "${1:-}" ]; then
    (get-blender)
    (get-libraries)
    (build-module)
    (build-jna-mapper)
else
    $@
fi
