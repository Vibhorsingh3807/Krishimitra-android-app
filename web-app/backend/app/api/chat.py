from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from typing import Optional, List, Dict, Any
from app.llm.ollama_client import ollama_client
from app.llm.rag_engine import rag_engine

router = APIRouter()

class ChatRequest(BaseModel):
    prompt: str
    system_prompt: Optional[str] = None
    model: Optional[str] = None
    language: Optional[str] = "auto"
    zone_context: Optional[Dict[str, Any]] = None

class HealthResponse(BaseModel):
    status: str
    base_url: str
    available_models: List[str] = []
    default_model: str

@router.get("/health")
async def get_llm_health():
    """Check connection to Ollama LLM service"""
    return await ollama_client.check_health()

@router.post("/stream")
async def stream_chat(req: ChatRequest):
    """
    Streaming chat endpoint via Server-Sent Events (SSE).
    Retrieves grounded ICAR knowledge and streams tokens from big LLM.
    """
    # 1. RAG retrieval
    context_docs = rag_engine.search(req.prompt)

    # 2. Augment with zone context if asked from Digital Twin
    extra_system = req.system_prompt or ""
    if req.zone_context:
        extra_system += (
            f"\n[Farmer Field Twin Context]: Zone {req.zone_context.get('id', 'Z-07')}, "
            f"Moisture: {req.zone_context.get('moisture', 18)}%, "
            f"NDVI: {req.zone_context.get('ndvi', 0.49)}, "
            f"Temp: {req.zone_context.get('temperature', 36.0)}°C, "
            f"Health: {req.zone_context.get('crop_health', 'Poor')}."
        )

    async def event_generator():
        async for token in ollama_client.stream_chat(
            prompt=req.prompt,
            system_prompt=extra_system,
            model=req.model,
            context_docs=context_docs
        ):
            yield f"data: {token}\n\n"
        yield "data: [DONE]\n\n"

    return StreamingResponse(event_generator(), media_type="text/event-stream")
