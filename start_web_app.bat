@echo off
echo =========================================================
echo    Launching KrishiMitra Web Platform (Backend + Frontend)
echo =========================================================

start "KrishiMitra Backend (Port 8000)" cmd /c "%~dp0run_backend.bat"
timeout /t 3 /nobreak >nul
start "KrishiMitra Frontend (Port 5173)" cmd /c "%~dp0run_frontend.bat"
timeout /t 2 /nobreak >nul
start http://localhost:5173
echo Web platform launched at http://localhost:5173
