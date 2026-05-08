@echo off
cd /d "%~dp0backend"

:: Load .env file
for /F "tokens=*" %%i in ('type ..\.env ^| findstr /V "^#" ^| findstr /V "^$"') do set %%i

:: Start Spring Boot
mvn spring-boot:run
