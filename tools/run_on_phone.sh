#!/usr/bin/env bash
# Compile, installe et lance l'app sur le telephone (debogage sans fil)
# Usage : tools/run_on_phone.sh [ip:port]   (le port change apres un redemarrage du tel)
set -e
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$HOME/devtools/jdk17}"
ADB="$HOME/devtools/android-sdk/platform-tools/adb"

if [ -n "$1" ]; then
    "$ADB" connect "$1"
fi
"$ADB" get-state > /dev/null || { echo "Telephone non connecte. Lance : $0 IP:PORT"; exit 1; }

./gradlew installDebug --console=plain -q
"$ADB" shell am start -n mg.monargent/.MainActivity > /dev/null
echo "App installee et lancee"
