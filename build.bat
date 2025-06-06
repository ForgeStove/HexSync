@echo off
setlocal
set JAVA_HOME=C:\Utils\Java\JDK 17
set JAR=HexSync.jar
set MAIN_CLASS=com.forgestove.hexsync.HexSync
set OUT_DIR=out\package
set ICON=src\main\resources\icon.ico
set RUNTIME_DIR=out\runtime\release
set JAR_PATH=out\artifacts\%JAR%

@echo on
if exist %OUT_DIR% rmdir /s /q %OUT_DIR%

if exist %RUNTIME_DIR% rmdir /s /q %RUNTIME_DIR%

for /f "delims=" %%i in ('"%JAVA_HOME%\bin\jdeps" --multi-release 17 --print-module-deps --ignore-missing-deps %JAR_PATH%') do set MODULES=%%i

jlink --module-path "%JAVA_HOME%\jmods" --add-modules %MODULES% --output %RUNTIME_DIR% --strip-debug --compress=2 --no-header-files --no-man-pages

jpackage ^
	--input out\artifacts ^
	--name HexSync ^
	--main-jar %JAR% ^
	--main-class %MAIN_CLASS% ^
	--icon %ICON% ^
	--type app-image ^
	--runtime-image %RUNTIME_DIR% ^
	--dest %OUT_DIR% ^
	--vendor "ForgeStove" ^
	--description "HexSync" ^
	--copyright "Copyright (c) 2025 ForgeStove" ^
	--app-version "1.0.0"

del "%OUT_DIR%\HexSync\HexSync.ico"