@echo off
REM Bisq Trusted Node Launcher for Windows
REM Reads configuration from trusted-node.properties and starts the node

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set PROPERTIES_FILE=%SCRIPT_DIR%trusted-node.properties

REM Check if properties file exists
if not exist "%PROPERTIES_FILE%" (
    echo Error: trusted-node.properties not found!
    echo Please create it from the template or check the README.md
    exit /b 1
)

REM Read properties with robust parsing
for /f "usebackq tokens=1* delims==" %%a in ("%PROPERTIES_FILE%") do (
    set "line=%%a"
    set "value=%%b"

    REM Skip empty lines and comments
    if not "!line!"=="" (
        if not "!line:~0,1!"=="#" (
            REM Trim whitespace from key
            for /f "tokens=* delims= " %%k in ("%%a") do set "key=%%k"

            REM Trim leading whitespace from value
            if not "!value!"=="" (
                for /f "tokens=* delims= " %%v in ("!value!") do set "value=%%v"
            )

            REM Assign using delayed expansion to preserve spaces in values
            if not "!key!"=="" (
                set "!key!=!value!"
            )
        )
    )
)

REM Validate required fields
if "%appName%"=="" (
    echo Error: appName is not set in trusted-node.properties
    exit /b 1
)

if "%password%"=="" (
    echo Error: password is not set in trusted-node.properties
    exit /b 1
)

if "%password%"=="CHANGE_ME_TO_A_STRONG_PASSWORD" (
    echo Error: Please set a strong password in trusted-node.properties
    exit /b 1
)

REM Build JAVA_OPTS
set JAVA_OPTS=-Dapplication.appName=%appName%
set JAVA_OPTS=%JAVA_OPTS% -Dapplication.websocket.password=%password%
set JAVA_OPTS=%JAVA_OPTS% -Dapplication.devMode=%devMode%

if not "%port%"=="" (
    set JAVA_OPTS=%JAVA_OPTS% -Dapplication.websocket.server.port=%port%
)

REM Parse transport types
if not "%transportTypes%"=="" (
    set INDEX=0
    for %%t in (%transportTypes:,= %) do (
        set JAVA_OPTS=!JAVA_OPTS! -Dapplication.network.supportedTransportTypes.!INDEX!=%%t
        set /a INDEX+=1
    )
)

REM Display configuration
echo ==========================================
echo Starting Bisq Trusted Node
echo ==========================================
echo Instance Name: %appName%
echo Port: %port%
echo Transport Types: %transportTypes%
echo Data Directory: %USERPROFILE%\.local\share\%appName%
echo ==========================================
echo.

REM Run the application
"%SCRIPT_DIR%bin\http-api-app.bat"

