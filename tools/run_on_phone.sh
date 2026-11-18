#!/usr/bin/env bash
# Compile, installe et lance l'app sur le telephone (debogage sans fil)
# Usage : tools/run_on_phone.sh [ip:port]
# Sans argument : se reconnecte tout seul (cherche le nouveau port si besoin)
set -e
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$HOME/devtools/jdk17}"
ADB="$HOME/devtools/android-sdk/platform-tools/adb"
PHONE_IP="${PHONE_IP:-192.168.88.229}"

if [ -n "$1" ]; then
    "$ADB" connect "$1"
fi

if ! "$ADB" get-state > /dev/null 2>&1; then
    echo "Recherche du telephone sur $PHONE_IP..."
    for addr in $(python3 -I tools/find_phone.py "$PHONE_IP"); do
        "$ADB" connect "$addr" | grep -q "connected to" && break
    done
fi
"$ADB" get-state > /dev/null || { echo "Telephone introuvable. Debogage sans fil active ?"; exit 1; }

./gradlew installDebug --console=plain -q
"$ADB" shell am start -n mg.monargent/.MainActivity > /dev/null
echo "App installee et lancee"
