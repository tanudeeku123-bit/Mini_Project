@echo off
cd /d "%~dp0"
echo Compiling Smart Public Transportation Route Optimization System...
javac -cp ".;lib/*" *.java
if %ERRORLEVEL% EQU 0 (
    echo Compilation successful!
) else (
    echo Compilation failed.
)
pause

