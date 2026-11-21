#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
: "${GRAVIT_CLASSPATH:?Set GRAVIT_CLASSPATH to absolute paths to Gravit jars}"
bash build.sh
javac --release 21 -cp "$GRAVIT_CLASSPATH:build/classes" -d build/test test/gdworld/android/*.java
build_dir="$PWD/build"
test_dir="$(mktemp -d)"
trap 'rm -rf "$test_dir"' EXIT
cd "$test_dir"
java -ea -cp "$GRAVIT_CLASSPATH:$build_dir/classes:$build_dir/test" gdworld.android.AndroidLauncherTest
