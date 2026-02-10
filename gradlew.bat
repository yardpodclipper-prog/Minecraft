@echo off
setlocal

REM Lightweight, text-only Gradle wrapper shim.
REM Uses a locally installed gradle executable to avoid checking in binary wrapper jars.

where gradle >nul 2>nul
if errorlevel 1 (
  echo ERROR: 'gradle' command not found in PATH.
  echo Install Gradle 8.14.3 or use an environment with Gradle preinstalled.
  exit /b 1
)

gradle %*
