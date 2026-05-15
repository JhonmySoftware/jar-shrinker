@echo off
title JAR Optimizer v1.0
color 0C

echo ============================================
echo   JAR OPTIMIZER
echo   Optimizador Inteligente de JARs
echo ============================================
echo.

set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%jar-shrinker-1.0.0.jar"

if not exist "%JAR%" (
    echo [ERROR] No se encuentra %JAR%
    echo Coloca este .bat junto al jar-shrinker-1.0.0.jar
    pause
    exit /b 1
)

echo [INFO] Iniciando...
start "" javaw -jar "%JAR%"
exit 0
