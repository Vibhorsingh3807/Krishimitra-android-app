import os
from pydantic import BaseModel

class Settings(BaseModel):
    PROJECT_NAME: str = "KrishiMitra Web API"
    VERSION: str = "2.0.0"
    API_V1_STR: str = "/api/v1"
    
    # Ollama LLM Configuration (e.g. 8B / 9GB+ models)
    OLLAMA_BASE_URL: str = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
    OLLAMA_MODEL: str = os.getenv("OLLAMA_MODEL", "llama3:8b")
    OLLAMA_TIMEOUT_SECONDS: float = float(os.getenv("OLLAMA_TIMEOUT", "60.0"))
    
    # Optional Cloud LLM Fallback (OpenAI / Groq)
    OPENAI_API_KEY: str = os.getenv("OPENAI_API_KEY", "")
    GROQ_API_KEY: str = os.getenv("GROQ_API_KEY", "")
    
    # CORS
    CORS_ORIGINS: list[str] = [
        "http://localhost:5173",
        "http://localhost:3000",
        "https://krishimitra.vercel.app",
        "*"
    ]

settings = Settings()
