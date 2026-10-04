@echo off
rem Seller tool for activation keys.   license-tool.bat init                  (once)
rem                                    license-tool.bat sign CODE [customer]  (for each sale)
cd /d "%~dp0"
java tools\license\LicenseTool.java %*
if "%~1"=="" pause
