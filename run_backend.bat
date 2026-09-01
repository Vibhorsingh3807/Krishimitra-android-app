@echo off
echo ==============================================
echo   Starting KrishiMitra Web Backend (FastAPI)
echo ==============================================
cd /d "%~dp0web-app\backend"
python -m uvicorn app.main:app --reload --port 8000 --host 127.0.0.1
pause
