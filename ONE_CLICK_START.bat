@echo off
title EcoRoute - Smart Public Waste Monitoring System (1-Click Launcher)
cls
echo =======================================================================
echo    ECO-ROUTE: SMART PUBLIC WASTE MONITORING SYSTEM (1-CLICK RUNNER)
echo =======================================================================
echo.

set ROOT_DIR=%~dp0
cd /d "%ROOT_DIR%"

echo [Step 1] Checking system environment...
where javac >nul 2>&1
if %errorlevel% equ 0 (
    echo [OK] Java Development Kit (JDK) detected on your computer!
    echo.
    echo [Step 2] Compiling Java backend classes...
    if not exist "bin" mkdir bin
    javac -encoding UTF-8 -d bin src\main\java\com\smartwaste\model\*.java src\main\java\com\smartwaste\service\*.java src\main\java\com\smartwaste\server\*.java src\main\java\com\smartwaste\*.java
    
    if %errorlevel% equ 0 (
        echo [OK] Java compilation succeeded!
        echo.
        echo [Step 3] Starting Java REST Server and opening your browser...
        timeout /t 1 /nobreak >nul
        start "" "http://localhost:8080"
        java -cp bin com.smartwaste.SmartWasteApplication 8080
        goto END
    ) else (
        echo [NOTICE] Java compiler had a notice. Falling back to Standalone Mode...
    )
)

echo.
echo [INFO] Launching instantly in Standalone Browser Mode...
echo (Includes full interactive GIS map, IoT sensor simulator, and route optimizer)
timeout /t 1 /nobreak >nul
start "" "%ROOT_DIR%frontend\index.html"

:END
