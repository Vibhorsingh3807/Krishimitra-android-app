import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.responses import HTMLResponse
from fastapi.middleware.cors import CORSMiddleware
from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.app.api.router import api_router
from backend.app.db.seed_data import seed_database

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Initializing KrishiMitra database and verified knowledge records...")
    seed_database()
    logger.info("Database initialized successfully.")
    yield

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan
)

# Enable CORS for local Android development and mobile emulators
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(api_router, prefix=settings.API_V1_STR)

@app.get("/")
def root():
    return {
        "app": "KrishiMitra (कृषिमित्र) - AI Agriculture Assistant Backend",
        "phone_helpline_simulator": "/phone",
        "docs": "/docs",
        "health": f"{settings.API_V1_STR}/health"
    }

@app.get("/phone", response_class=HTMLResponse)
def phone_helpline_simulator():
    """Interactive Web Phone Helpline Simulator with speech synthesis and voice recognition"""
    template_path = os.path.join(os.path.dirname(__file__), "templates", "phone_simulator.html")
    with open(template_path, "r", encoding="utf-8") as f:
        return f.read()

if __name__ == "__main__":
    import uvicorn
    uvicorn.run("backend.app.main:app", host=settings.HOST, port=settings.PORT, reload=settings.DEBUG)
