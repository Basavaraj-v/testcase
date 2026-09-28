@echo off
title EcoRoute Smart Waste Monitoring System - Java Launcher
cls
echo ======================================================================
echo    ECO-ROUTE: SMART PUBLIC WASTE MONITORING SYSTEM (JAVA EDITION)
echo ======================================================================
echo.

where javac >nul 2>&1
if %errorlevel% neq 0 (
    echo [WARNING] Java Development Kit (javac) was not found in your PATH.
    echo Opening Standalone Interactive Dashboard in your default browser...
    echo.
    start "" "%~dp0frontend\index.html"
    pause
    exit /b
)

echo [1/3] Creating output directory 'bin'...
if not exist "bin" mkdir bin

echo [2/3] Compiling Java classes with standard JDK...
javac -encoding UTF-8 -d bin src\main\java\com\smartwaste\model\*.java src\main\java\com\smartwaste\service\*.java src\main\java\com\smartwaste\server\*.java src\main\java\com\smartwaste\*.java

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Java compilation failed.
    echo Falling back to standalone browser preview...
    start "" "%~dp0frontend\index.html"
    pause
    exit /b
)

echo [3/3] Starting Java REST API and Web Server on port 8080...
start "" http://localhost:8080
java -cp bin com.smartwaste.SmartWasteApplication 8080

pause
