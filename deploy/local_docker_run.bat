@echo off
setlocal

set "SCRIPT_DIR=%~dp0"
set "LOCAL_DELETE_DB="
set "LOCAL_ARGS="

:parse_args
if "%~1"=="" goto run_local
if /I "%~1"=="--delete-db" (
    set "LOCAL_DELETE_DB=1"
    shift
    goto parse_args
)
set "LOCAL_ARGS=%LOCAL_ARGS% "%~1""
shift
goto parse_args

:run_local
python "%SCRIPT_DIR%local.py" %LOCAL_ARGS%
if errorlevel 1 exit /b %errorlevel%
