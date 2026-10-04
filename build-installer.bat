@echo off
rem One-click build: Spring Boot JAR  ->  app with bundled Java (jpackage)  ->  MyPOS-Setup.exe (Inno Setup)
rem Usage:  build-installer.bat          (final build, no console window)
rem         build-installer.bat debug    (keeps a console window, to see startup errors)
setlocal
cd /d "%~dp0"

set APPVER=1.0.0
set CONSOLE=
if /i "%1"=="debug" set CONSOLE=--win-console

set "ISCC=%ProgramFiles(x86)%\Inno Setup 6\ISCC.exe"
if not exist "%ISCC%" set "ISCC=%ProgramFiles%\Inno Setup 6\ISCC.exe"
if not exist "%ISCC%" (echo Inno Setup 6 was not found. Install it first. & goto fail)

if not exist "src\main\resources\license-public.key" (echo No license key found. Run  license-tool.bat init  once, then run this file again. & goto fail)

where jpackage >nul 2>nul
if errorlevel 1 (echo jpackage was not found. Use a full JDK 17 or newer. & goto fail)

echo.
echo [1/4] Building the JAR...
call mvn clean package -DskipTests
if errorlevel 1 goto fail

set JAR=
for %%f in (target\pos-*.jar) do set JAR=%%~nxf
if "%JAR%"=="" (echo No JAR found in target. & goto fail)

echo.
echo [2/4] Preparing build folder...
if exist build rmdir /s /q build
mkdir build\input
copy /y "target\%JAR%" build\input\pos.jar >nul
if errorlevel 1 goto fail

echo.
echo [3/4] Creating the app with bundled Java (jpackage)...
jpackage --type app-image --name MyPOS --app-version %APPVER% ^
 --input build\input --main-jar pos.jar --dest build\out ^
 --add-modules java.se,jdk.unsupported,jdk.crypto.ec,jdk.management,jdk.zipfs ^
 --java-options "-Xms128m -Xmx512m -XX:+UseSerialGC -XX:TieredStopAtLevel=1" ^
 --java-options "-Dapp.license.enabled=true" %CONSOLE%
if errorlevel 1 goto fail

echo.
echo [4/4] Creating the installer (Inno Setup)...
"%ISCC%" /DAppVersion=%APPVER% installer\MyPOS.iss
if errorlevel 1 goto fail

echo.
echo DONE. Your installer is here:  build\installer\MyPOS-Setup-%APPVER%.exe
pause
exit /b 0

:fail
echo.
echo BUILD FAILED. Read the messages above.
pause
exit /b 1
