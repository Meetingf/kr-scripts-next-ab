#!/system/bin/sh

busybox_uninstall() {

    cd "$TOOLKIT" || exit 1

    if [ ! -f "./busybox" ]; then
        echo "BusyBox not found." >&2
        return 1
    fi

    busybox_inode=$(stat -Lc %i "./busybox" 2>/dev/null)
    if [ -z "$busybox_inode" ]; then
        echo "Cannot read inode." >&2
        return 1
    fi

    for applet in $(./busybox --list 2>/dev/null); do
        if [ -e "./$applet" ]; then
            applet_inode=$(stat -Lc %i "./$applet" 2>/dev/null)
            if [ "$busybox_inode" = "$applet_inode" ]; then
                rm -f "./$applet"
                echo "rm: $applet" >&2
            else
                echo "skip (inode not match): $applet" >&2
            fi
        fi
    done

    rm -f "./busybox_installed"
    echo "Done"
}

busybox_version() {
    echo '当前busybox版本:'
    busybox | grep BusyBox
    echo ''
    echo '当前目录:'
    pwd
}

root_state() {
    if [ "$(id -u 2>/dev/null)" -eq 0 ] 2>/dev/null; then
        echo 'user: Root'
    else
        echo 'user: Not Root'
    fi

    echo "uid: $(id -u 2>&1)"
    echo "whoami: $(whoami 2>&1)"
    echo "USER_ID: ${USER_ID:-未定义}"
    echo "ROOT_PERMISSION=${ROOT_PERMISSION}"
}

environment() {
    printf '框架定义\n'
    echo "EXECUTOR_PATH=$EXECUTOR_PATH"
    echo "START_DIR=$START_DIR"
    echo "TEMP_DIR=$TEMP_DIR"
    echo "ANDROID_UID=$ANDROID_UID"
    echo "ANDROID_SDK=$ANDROID_SDK"
    echo "SDCARD_PATH=$SDCARD_PATH"
    echo "PACKAGE_NAME=$PACKAGE_NAME"
    echo "PACKAGE_VERSION_NAME=$PACKAGE_VERSION_NAME"
    echo "PACKAGE_VERSION_CODE=$PACKAGE_VERSION_CODE"
    echo "APP_USER_ID=$APP_USER_ID"
    echo "ROOT_PERMISSION=$ROOT_PERMISSION"
    echo "TOOLKIT=$TOOLKIT"

    printf '\nenv 命令\n'
    env

    printf '\nset 命令\n'
    set

    printf '\nexport -p 命令\n'
    export -p
}

config_path() {

    echo 'PAGE_CONFIG_DIR [配置XML来源目录]'
    echo "$PAGE_CONFIG_DIR"
    echo ''

    echo 'PAGE_CONFIG_FILE [配置XML来源路径]'
    echo "$PAGE_CONFIG_FILE"
    echo ''

    echo 'PAGE_WORK_DIR [配置XML提取目录]'
    echo "$PAGE_WORK_DIR"
    echo ''

    echo 'PAGE_WORK_FILE [配置XML提取目录]'
    echo "$PAGE_WORK_FILE"
    echo ''
}

$1
