@echo off
setlocal

REM 设置变量
set JAVA_HOME=C:\Utils\Java\JDK 17
set JAR=HexSync.jar
set MAIN_CLASS=com.forgestove.hexsync.HexSync
set OUT_DIR=out\HexSync
set ICON=src\main\resources\icon.ico
set RUNTIME_DIR=out\runtime\release
set JAR_PATH=out\artifacts\%JAR%

REM 检查 JAVA_HOME
if "%JAVA_HOME%"=="" (
    echo 请先设置 JAVA_HOME 环境变量为JDK目录
    goto end
)

REM 删除旧的 app-image
if exist %OUT_DIR% rmdir /s /q %OUT_DIR%

REM 删除旧的 runtime
if exist %RUNTIME_DIR% rmdir /s /q %RUNTIME_DIR%

REM 自动分析依赖模块
for /f "delims=" %%i in ('"%JAVA_HOME%\bin\jdeps" --multi-release 17 --print-module-deps --ignore-missing-deps %JAR_PATH%') do set MODULES=%%i

REM 用 jlink 生成精简 runtime
"%JAVA_HOME%\bin\jlink" --module-path "%JAVA_HOME%/jmods" --add-modules %MODULES% --output %RUNTIME_DIR% --strip-debug --compress=2 --no-header-files --no-man-pages

REM 执行 jpackage 打包，指定 runtime
jpackage ^
  --input out\artifacts ^
  --name HexSync ^
  --main-jar %JAR% ^
  --main-class %MAIN_CLASS% ^
  --icon %ICON% ^
  --type app-image ^
  --runtime-image %RUNTIME_DIR% ^
  --dest out\package ^