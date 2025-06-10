#!/bin/bash
if [[ "$(java -version 2>&1)" == *"17."* ]]; then
  echo "已检测到 JDK 17，跳过设置 JAVA_HOME"
else
  JAVA_HOME="C:/Utils/Java/JDK 17"
  if [ -d "$JAVA_HOME" ]; then
    echo "设置 JAVA_HOME 为 $JAVA_HOME"
    export JAVA_HOME
    export PATH="$JAVA_HOME/bin:$PATH"
  else
    echo "错误: JDK 17 路径不存在: $JAVA_HOME"
    read -n 1 -s -r -p "按任意键退出..."
    exit 1
  fi
fi
JAR="HexSync.jar"
MAIN_CLASS="com.forgestove.hexsync.HexSync"
INPUT_DIR="build/libs"
OUT_DIR="build/package"
ICON="icon.ico"
RUNTIME_DIR="build/runtime"
JAR_PATH="$INPUT_DIR/$JAR"

if [ -d "$OUT_DIR" ]; then
  echo "删除旧的输出目录 $OUT_DIR..."
  rm -rf "$OUT_DIR"
  echo "已删除 $OUT_DIR"
fi

if [ -d "$RUNTIME_DIR" ]; then
  echo "删除旧的运行时目录 $RUNTIME_DIR..."
  rm -rf "$RUNTIME_DIR"
  echo "已删除 $RUNTIME_DIR"
fi

echo "正在分析依赖模块..."
MODULES=$("$JAVA_HOME"/bin/jdeps --multi-release 17 --print-module-deps --ignore-missing-deps "$JAR_PATH")
echo "依赖模块: $MODULES"

echo "正在使用 jlink 构建自定义运行时..."
"$JAVA_HOME"/bin/jlink \
  --module-path "$JAVA_HOME/jmods" \
  --add-modules "$MODULES" \
  --output "$RUNTIME_DIR" \
  --strip-debug \
  --compress=2 \
  --no-header-files \
  --no-man-pages
echo "自定义运行时已生成: $RUNTIME_DIR"

echo "正在使用 jpackage 打包应用..."
"$JAVA_HOME"/bin/jpackage \
  --input $INPUT_DIR \
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
echo "应用已打包到: $OUT_DIR"

echo "清理临时运行时目录..."
rm -rf "$RUNTIME_DIR"
echo "已删除 $RUNTIME_DIR"

echo "删除多余的图标文件..."
rm -f "$OUT_DIR/HexSync/HexSync.ico"
echo "已删除 $OUT_DIR/HexSync/HexSync.ico"
read -n 1 -s -r -p "按任意键退出..."
