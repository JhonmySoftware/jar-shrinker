@echo off
title JAR Optimizer v1.0.2

set "SCRIPT_DIR=%~dp0"
set "JAR=%SCRIPT_DIR%jar-shrinker-1.0.2.jar"

if not exist "%JAR%" (
    echo [ERROR] No se encuentra %JAR%
    echo Coloca este .bat junto al jar-shrinker-1.0.2.jar
    pause
    exit /b 1
)

start "" javaw -jar "%JAR%"
exit 0
