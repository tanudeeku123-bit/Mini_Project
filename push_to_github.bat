@echo off
cd /d "%~dp0"
echo ===================================================
echo Pushing Smart Public Transportation to GitHub
echo Repository: https://github.com/tanudeeku123-bit/Mini_Project.git
echo Branch:     main
echo ===================================================
echo.
git push -u origin main
echo.
if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Push completed successfully!
) else (
    echo [NOTICE] If prompted by Git Credential Manager, complete the sign-in in your browser.
)
pause
