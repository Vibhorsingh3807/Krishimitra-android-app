import os
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "KrishiMitra Backend API"
    VERSION: str = "1.0.0"
    API_V1_STR: str = "/api/v1"
    ENVIRONMENT: str = "development"
    DEBUG: bool = True
    PORT: int = 8000
    HOST: str = "0.0.0.0"

    DATABASE_URL: str = "sqlite:///./krishimitra.db"

    AI_FALLBACK_PROVIDER: str = "local"  # 'local', 'openai', 'gemini'
    OPENAI_API_KEY: str = ""
    OPENAI_MODEL: str = "gpt-4o-mini"

    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-1.5-flash"

    WEATHER_PROVIDER: str = "open_meteo"

    # Twilio Telephony Settings
    TWILIO_ACCOUNT_SID: str = ""
    TWILIO_AUTH_TOKEN: str = ""
    TWILIO_PHONE_NUMBER: str = ""
    TELEPHONY_DEFAULT_LANGUAGE: str = "hi-IN"
    TELEPHONY_TTS_VOICE: str = "Polly.Aditi"  # Indian Hindi voice
    TELEPHONY_SPEECH_TIMEOUT: str = "auto"
    TELEPHONY_SEND_SMS_SUMMARY: bool = True
    BASE_WEBHOOK_URL: str = ""  # e.g., https://your-ngrok-id.ngrok-free.app

    class Config:
        env_file = ".env"
        extra = "ignore"

settings = Settings()
