#!/bin/sh
# Запускает готовую битву со встроенной Linux Java и отдельными данными пользователя.
set -eu

fail() {
    printf '%s\n' "$*" >&2
    exit 1
}

[ "$(uname -s)" = Linux ] || fail 'This package requires Linux.'
[ "$(uname -m)" = x86_64 ] || fail 'This package requires Linux x86_64.'

APP_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)
JAVA="$APP_DIR/runtime/bin/java"
[ -x "$JAVA" ] || fail 'Bundled Java is missing or not executable. Extract the complete tar.gz archive on Linux.'
[ -r "$APP_DIR/HeroicCivsBattle.jar" ] || fail 'HeroicCivsBattle.jar is missing.'

# Относительный XDG_DATA_HOME не используем: путь не должен зависеть от места запуска.
case "${XDG_DATA_HOME:-}" in
    /*) DATA_ROOT=$XDG_DATA_HOME ;;
    *) DATA_ROOT="${HOME:?HOME must be set}/.local/share" ;;
esac
DATA_DIR="$DATA_ROOT/HeroicCivsBattle"
mkdir -p -- "$DATA_DIR"
cd -- "$DATA_DIR"
exec "$JAVA" -Xmx1G -jar "$APP_DIR/HeroicCivsBattle.jar" "$@"
