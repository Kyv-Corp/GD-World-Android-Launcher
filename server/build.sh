#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${GRAVIT_CLASSPATH:?Set GRAVIT_CLASSPATH to the Gravit 5.7.14 distribution jars}"
mkdir -p build/classes
javac --release 21 -cp "$GRAVIT_CLASSPATH" -d build/classes src/gdworld/android/*.java
printf 'Manifest-Version: 1.0\nModule-Main-Class: gdworld.android.GDWorldAndroidModule\n\n' > build/MANIFEST.MF
jar cfm build/GDWorldAndroid_module.jar build/MANIFEST.MF -C build/classes .
