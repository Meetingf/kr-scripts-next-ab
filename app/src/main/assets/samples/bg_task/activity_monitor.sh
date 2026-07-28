#!/system/bin/sh

watcher_start() {
  lst_app=""

  echo 'History:'

  am monitor | while read -r line
  do
    case $line in *Activity*)
        current_app=$(echo "$line" | cut -f2 -d ':')
        if [ ! "$current_app" = "$lst_app" ]; then
            echo "$current_app"
            lst_app="$current_app"
        fi
    esac
  done
}

watcher_start
