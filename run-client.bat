@echo off
REM Starts a DRS-Enhanced JavaFX client (start the server first). Run several times for several users.
cd /d "%~dp0"
start "" javaw -jar target\DRS-Enhanced-1.0.0-all.jar
