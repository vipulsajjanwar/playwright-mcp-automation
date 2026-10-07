@echo off
setlocal enabledelayedexpansion

:: Enable color output
for /f %%a in ('powershell -command "$host.UI.RawUI.ForegroundColor"') do set COLOR_DEFAULT=%%a

:: ========== Fancy Logging Functions ==========
set "LOG_INFO=[36m[INFO][0m"
set "LOG_WARN=[33m[WARN][0m"
set "LOG_ERR=[31m[ERROR][0m"
set "LOG_OK=[32m[ OK ][0m"

echo [95m============================================================[0m
echo.
echo       [3m[92m  Automation Dashboard  [0m
echo.
echo [95m============================================================[0m
echo.

:: Step 1: Get current Windows directory
set "WIN_DIR=%~dp0"
set "WIN_DIR=%WIN_DIR:~0,-1%"

:: Step 2: Build WSL-compatible path
set "DRIVE_LETTER=%WIN_DIR:~0,1%"
set "PATH_PART=%WIN_DIR:~2%"
set "PATH_PART=%PATH_PART:\=/%"
set "TEMP_WSL_PATH=/mnt/%DRIVE_LETTER%%PATH_PART%"

:: Step 3: Convert to lowercase for Linux
for /f "usebackq delims=" %%i in (`powershell -nologo -command "'%TEMP_WSL_PATH%'.ToLower()"`) do set "WSL_PATH=%%i"

:: Display paths
echo %LOG_INFO% Windows Path: %WIN_DIR%
echo %LOG_INFO% WSL Path:     %WSL_PATH%
echo.

:: Step 4: Start WSL
echo %LOG_INFO% Initializing WSL environment...
wsl echo "WSL Initialized."

:: Wait for WSL to be ready
timeout /t 2 >nul

:: Step 5: Run docker-compose with multiple files
echo %LOG_INFO% Launching Docker containers...
wsl bash -c "cd '%WSL_PATH%' && echo '[WSL] Running docker compose...' && docker compose -f docker-compose-elksetup-n2.yaml up -d"

:: Final message
echo.
echo [95m================================================[0m
echo      [97mDocker containers launched via WSL[0m
echo [95m================================================[0m
echo.
pause
