@echo off
rem Downloads the JavaScript/CSS libraries the POS pages need, so the app works with NO internet.
rem Run this ONCE (needs internet), from the project folder, then rebuild the project.
setlocal
set "JS=%~dp0src\main\resources\static\js"
set "CSS=%~dp0src\main\resources\static\css"
if not exist "%JS%" mkdir "%JS%"
if not exist "%CSS%" mkdir "%CSS%"

set FAILED=0
call :get "https://unpkg.com/htmx.org@1.9.10/dist/htmx.min.js" "%JS%\htmx.min.js"
call :get "https://unpkg.com/htmx.org@1.9.10/dist/ext/response-targets.js" "%JS%\htmx-response-targets.js"
call :get "https://cdn.jsdelivr.net/npm/flatpickr@4.6.13/dist/flatpickr.min.js" "%JS%\flatpickr.min.js"
call :get "https://cdn.jsdelivr.net/npm/jsbarcode@3.11.5/dist/JsBarcode.all.min.js" "%JS%\JsBarcode.all.min.js"
call :get "https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.min.css" "%CSS%\pico.min.css"
call :get "https://cdn.jsdelivr.net/npm/water.css@2/out/water.css" "%CSS%\water.css"
call :get "https://cdn.jsdelivr.net/npm/flatpickr@4.6.13/dist/flatpickr.min.css" "%CSS%\flatpickr.min.css"

echo.
if "%FAILED%"=="0" (echo ALL 7 FILES DOWNLOADED. You can rebuild the project now.) else (echo %FAILED% files FAILED. Check your internet and run this file again.)
pause
exit /b

:get
curl.exe -fsSL -o %2 %1
if errorlevel 1 (echo FAILED: %1 & set /a FAILED+=1) else (echo OK:     %~nx2)
exit /b
