from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.api import chat, twin, disease, weather, crops, schemes, mandi, rental

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description="KrishiMitra Web API — Cloud Backend with Big LLM (Ollama) and Field Digital Twin"
)

# Enable CORS for Vercel Frontend and local dev
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.CORS_ORIGINS,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register Sub-routers
app.include_router(chat.router, prefix="/api/v1/ai", tags=["AI & Ollama LLM"])
app.include_router(twin.router, prefix="/api/v1/twin", tags=["Field Digital Twin"])
app.include_router(disease.router, prefix="/api/v1/disease", tags=["Leaf Disease Diagnosis"])
app.include_router(weather.router, prefix="/api/v1/weather", tags=["Live Weather & Spray Window"])
app.include_router(crops.router, prefix="/api/v1/crops", tags=["Crop Cultivation Manuals"])
app.include_router(schemes.router, prefix="/api/v1/schemes", tags=["Government Schemes & Loans"])
app.include_router(mandi.router, prefix="/api/v1/mandi", tags=["Mandi Prices & Trends"])
app.include_router(rental.router, prefix="/api/v1/rental", tags=["Equipment Rental"])

@app.get("/")
def root():
    return {
        "status": "online",
        "service": settings.PROJECT_NAME,
        "version": settings.VERSION,
        "ollama_configured": settings.OLLAMA_BASE_URL,
        "docs": "/docs"
    }

@app.get("/health")
def health():
    return {"status": "healthy"}
