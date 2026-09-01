# 🌾 KrishiMitra Web App (Final Edition)

A high-performance, full-stack agricultural platform featuring:
- **Frontend**: Deployable on **Vercel** (Vite + Vanilla JS + Glassmorphism Design System)
- **Backend**: Deployable on **Render** (FastAPI + Docker + Python 3.11)
- **Big LLM**: Native integration with **Ollama** (`llama3:8b`, `qwen2.5:7b`, `mistral`, `gemma2:9b` / 9GB+ models)
- **Field Digital Twin**: Interactive satellite imagery (Esri World Imagery) with 9 management zones, GPS auto-centering, and closed-loop irrigation action feedback.

---

## 🚀 1-Click Deployment Guide

### A. Deploy Backend on Render

1. Create a new **Web Service** on [Render](https://render.com).
2. Connect your GitHub repository: `https://github.com/Vibhorsingh3807/Krishimitra-android-app`.
3. Set the **Root Directory** to: `web-app/backend`.
4. Configure the build:
   - **Environment**: `Python`
   - **Build Command**: `pip install -r requirements.txt`
   - **Start Command**: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
5. Add Environment Variables:
   - `OLLAMA_BASE_URL`: `https://your-hosted-ollama.com` (or leave default for local)
   - `OLLAMA_MODEL`: `llama3:8b` (or `qwen2.5:7b`, `mistral`)
6. Deploy! Render will give you a URL like: `https://krishimitra-web-backend.onrender.com`.

### B. Deploy Frontend on Vercel

1. Import your project on [Vercel](https://vercel.com).
2. Set the **Root Directory** to: `web-app/frontend`.
3. Framework Preset: `Vite` (automatically detected).
4. In `web-app/frontend/vercel.json`, verify the rewrite URL points to your Render backend:
   ```json
   {
     "rewrites": [
       {
         "source": "/api/:path*",
         "destination": "https://krishimitra-web-backend.onrender.com/api/:path*"
       }
     ]
   }
   ```
5. Click **Deploy**! Your web app is live on `https://your-app.vercel.app`.

---

## 💻 Running Locally

### 1. Start the Backend & Ollama
```bash
# In terminal 1 (Start Ollama with Llama 3 8B)
ollama run llama3:8b

# In terminal 2 (Start Backend)
cd web-app/backend
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```
Visit API Documentation: `http://localhost:8000/docs`

### 2. Start the Frontend
```bash
# In terminal 3 (Start Frontend)
cd web-app/frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser!

---

## 🌟 Key Functional Features

1. **🌾 Field Digital Twin**:
   - High-resolution real satellite layer (Esri World Imagery).
   - 9-zone management grid (`Z-01` to `Z-09`).
   - Zone 7 tracks 30-day water stress decline ($32\% \rightarrow 18\%$).
   - Click **"✓ सिंचाई दर्ज करें (Mark Irrigated)"** $\rightarrow$ closed-loop twin updates immediately to 32% moisture!
2. **🤖 Big LLM AI Assistant**:
   - Streaming SSE tokens with real-time typing animation.
   - Switch between `llama3:8b`, `qwen2.5:7b`, or `mistral`.
   - RAG grounded in verified ICAR agricultural manuals.
3. **🍃 Leaf Disease Scanner**:
   - Image analysis with organic and chemical remedies.
   - Tag scan observations directly to field twin zones.
4. **⛅ Weather & Spray Window**:
   - Open-Meteo live sync and wind/rain drift risk window.
5. **🌱 49 ICAR Crop Guides, Mandi APMC Prices, Smart Loan Matcher, and Farm Equipment Rental**.
