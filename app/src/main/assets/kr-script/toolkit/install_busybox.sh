#!/system/bin/sh

busybox_install() {
    ./busybox --install -s ./
    echo '' > busybox_installed
}

if [ ! "$TOOLKIT" = "" ]; then
    cd "$TOOLKIT" || exit 1
    if [ ! -f busybox_installed ]; then
        busybox_install
    fi
fi
