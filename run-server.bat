@echo off
REM Starts the DRS-Enhanced multi-threaded server (build the project first: mvn clean package)
cd /d "%~dp0"
java -cp target\DRS-Enhanced-1.0.0-all.jar com.cqu.drs.server.ServerMain
pause
