@echo off
cd /d "%~dp0"
echo Starting Smart Public Transportation Route Optimization System...
java -cp ".;lib/*" Main
pause

