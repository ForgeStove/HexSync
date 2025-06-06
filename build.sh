#!/bin/bash
set -e

JAVA_HOME="C:/Utils/Java/JDK 17"
JAR="HexSync.jar"
MAIN_CLASS="com.forgestove.hexsync.HexSync"
OUT_DIR="out/package"
ICON="icon.ico"
RUNTIME_DIR="out/runtime"
JAR_PATH="out/artifacts/$JAR"

if [ -d "$OUT_DIR" ]; then
  rm -rf "$OUT_DIR"
fi

MODULES=$("$JAVA_HOME"/bin/jdeps --multi-release 17 --print-module-deps --ignore-missing-deps "$JAR_PATH")

"$JAVA_HOME"/bin/jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "$MODULES" \
  --output "$RUNTIME_DIR" \
  --strip-debug \
  --compress=2 \
  --no-header-files \
  --no-man-pages

"$JAVA_HOME"/bin/jpackage \
  --input out/artifacts \
  --name HexSync \
  --main-jar "$JAR" \
  --main-class "$MAIN_CLASS" \
  --icon "$ICON" \
  --type app-image \
  --runtime-image "$RUNTIME_DIR" \
  --dest "$OUT_DIR" \
  --vendor "ForgeStove" \
  --description "HexSync" \
  --copyright "Copyright (c) 2025 ForgeStove" \
  --app-version "1.0.0"

rm -rf "$RUNTIME_DIR"
rm -f "$OUT_DIR/HexSync/HexSync.ico"