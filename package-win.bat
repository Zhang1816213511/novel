@echo off
chcp 65001 > nul
title NovelWriter 桌面客户端打包 (Electron)

echo ============================================
echo  NovelWriter 桌面客户端打包 (Electron)
echo ============================================
echo.

:: ======== 配置区域 ========
set "JPKG_JAVA_HOME=D:\app\jdk\jdk-21"
set "APP_NAME=NovelWriter"
set "APP_VERSION=1.0.0"
set "MAIN_JAR=novel-%APP_VERSION%.jar"
set "MAIN_CLASS=com.novel.NovelApplication"
:: ===========================

set "PROJECT_DIR=%CD%"
set "WEB_DIR=%PROJECT_DIR%\web"
set "TARGET_DIR=%PROJECT_DIR%\target"
set "RUNTIME_DIR=%PROJECT_DIR%\build-runtime"
set "STAGE_DIR=%PROJECT_DIR%\build-staging"
set "ELECTRON_DIR=%PROJECT_DIR%\electron-dist"

:: 检查必要工具
where node >nul 2>nul
if %errorlevel% neq 0 ( echo [错误] 未找到 node.exe & pause & exit /b 1 )
where npm >nul 2>nul
if %errorlevel% neq 0 ( echo [错误] 未找到 npm & pause & exit /b 1 )

echo.
echo [1/4] 构建 Vue 前端...
cd /d "%WEB_DIR%"
if not exist "node_modules" call npm install
call npm run build
if %errorlevel% neq 0 ( echo [错误] 前端打包失败! & pause & exit /b 1 )
echo   OK - 前端已输出到 java/src/main/resources/static/
echo.

echo [2/4] 构建 Spring Boot 后端...
cd /d "%PROJECT_DIR%"
set "JAVA_HOME=%JPKG_JAVA_HOME%"
call mvn clean package -DskipTests -Dmaven.test.skip=true -s "D:\app\apache-maven-3.9.9\conf\alyun.xml"
if %errorlevel% neq 0 ( echo [错误] 后端打包失败! & pause & exit /b 1 )
echo   OK - JAR: %TARGET_DIR%\%MAIN_JAR%
echo.

echo [3/4] 创建最小 JRE 运行时 (jlink)...
if exist "%RUNTIME_DIR%" rmdir /s /q "%RUNTIME_DIR%"
"%JPKG_JAVA_HOME%\bin\jlink" ^
    --module-path "%JPKG_JAVA_HOME%\jmods" ^
    --add-modules java.base,java.compiler,java.datatransfer,java.desktop,java.instrument,java.logging,java.management,java.naming,java.net.http,java.prefs,java.rmi,java.scripting,java.security.jgss,java.security.sasl,java.sql,java.transaction.xa,java.xml,jdk.httpserver,jdk.jfr,jdk.unsupported,jdk.zipfs ^
    --output "%RUNTIME_DIR%" ^
    --strip-debug --no-man-pages --no-header-files --compress=2
if %errorlevel% neq 0 ( echo [错误] jlink 创建运行时失败! & pause & exit /b 1 )
echo   OK - JRE 运行时: %RUNTIME_DIR%
echo.

echo [4/4] 构建 Electron 桌面客户端...
cd /d "%PROJECT_DIR%"

:: 设置镜像（国内加速）
set ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/
set ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/

:: 检查 electron 依赖
if not exist "node_modules" call npm install

:: 先创建目录输出（便携版，不含安装包）
call npm run electron:build:dir
if %errorlevel% equ 0 (
    echo   OK - 便携版构建成功!
) else (
    echo   [错误] Electron 构建失败!
    pause
    exit /b 1
)

:: 创建完整安装包 (NSIS installer .exe)
echo.
echo   正在创建安装包...
call npm run electron:build
if %errorlevel% equ 0 (
    echo   OK - 安装包构建成功!
) else (
    echo   [警告] 安装包构建失败，便携版可用
)

:: 清理
if exist "%RUNTIME_DIR%" rmdir /s /q "%RUNTIME_DIR%"

echo.
echo ============================================
echo  ✓ 打包完成!
echo ============================================
echo.
if exist "%ELECTRON_DIR%\win-unpacked" (
    echo  便携版: %ELECTRON_DIR%\win-unpacked\NovelWriter.exe
    echo  直接双击运行即可
)
echo.
if exist "%ELECTRON_DIR%\*.exe" (
    for %%f in ("%ELECTRON_DIR%\*.exe") do (
        echo  安装包: %%f
    )
)
echo.
echo  数据存储: %%APPDATA%%\NovelWriter\data
echo.
pause
