import json
import httpx
from typing import AsyncGenerator, Dict, Any, List
from app.config import settings

class OllamaClient:
    """
    Client for interacting with local or cloud-hosted Ollama LLM instances
    (e.g., Llama 3 8B, Qwen 2.5 7B, Mistral 7B).
    """

    def __init__(self, base_url: str = settings.OLLAMA_BASE_URL, default_model: str = settings.OLLAMA_MODEL):
        self.base_url = base_url.rstrip("/")
        self.default_model = default_model

    async def check_health(self) -> Dict[str, Any]:
        """Check if Ollama server is reachable and list loaded models"""
        try:
            async with httpx.AsyncClient(timeout=4.0) as client:
                res = await client.get(f"{self.base_url}/api/tags")
                if res.status_code == 200:
                    data = res.json()
                    models = [m.get("name") for m in data.get("models", [])]
                    return {
                        "status": "connected",
                        "base_url": self.base_url,
                        "available_models": models,
                        "default_model": self.default_model
                    }
        except Exception as e:
            return {
                "status": "offline",
                "base_url": self.base_url,
                "error": str(e),
                "notice": "Ollama server is currently offline or unreachable. Using grounded ICAR agronomic fallback."
            }
        return {"status": "unreachable"}

    async def is_available(self) -> bool:
        """Instant check if Ollama server is up"""
        try:
            async with httpx.AsyncClient(timeout=1.0) as client:
                res = await client.get(f"{self.base_url}/api/tags")
                return res.status_code == 200
        except Exception:
            return False

    async def stream_chat(
        self,
        prompt: str,
        system_prompt: str = "",
        model: str = None,
        context_docs: List[Dict[str, Any]] = None
    ) -> AsyncGenerator[str, None]:
        """
        Stream tokens from Ollama LLM with grounded RAG context.
        Falls back smoothly and instantly if Ollama is unreachable.
        """
        # If Ollama is not active, immediately stream from Agronomic Expert Agent (zero wait)
        if not await self.is_available():
            from app.llm.expert_agent import expert_agent
            async for token in expert_agent.stream_expert_response(prompt):
                yield token
            return

        selected_model = model or self.default_model
        
        # Assemble Grounded Agricultural Prompt
        full_system = (
            "You are KrishiMitra, an expert AI Agricultural Assistant developed for Indian farmers. "
            "Respond respectfully, clearly, and concisely in the same language the farmer asks (Hindi, Hinglish, or English). "
            "Provide practical recommendations covering soil, dosage, weather timing, and official ICAR guidelines. "
        )
        if system_prompt:
            full_system += f"\n{system_prompt}"

        if context_docs:
            context_text = "\n".join([f"• [{d.get('title', 'ICAR Document')}]: {d.get('text', '')}" for d in context_docs])
            prompt_with_context = (
                f"### Verified Agricultural Reference Knowledge:\n{context_text}\n\n"
                f"### Farmer Query:\n{prompt}\n\n"
                f"### Expert Guidance (cite verified ICAR source when relevant):"
            )
        else:
            prompt_with_context = prompt

        payload = {
            "model": selected_model,
            "prompt": prompt_with_context,
            "system": full_system,
            "stream": True,
            "options": {
                "temperature": 0.3,
                "top_p": 0.9,
                "num_ctx": 4096
            }
        }

        try:
            timeout = httpx.Timeout(timeout=60.0, connect=1.5)
            async with httpx.AsyncClient(timeout=timeout) as client:
                async with client.stream("POST", f"{self.base_url}/api/generate", json=payload) as response:
                    if response.status_code != 200:
                        from app.llm.expert_agent import expert_agent
                        async for token in expert_agent.stream_expert_response(prompt):
                            yield token
                        return

                    async for line in response.aiter_lines():
                        if line:
                            try:
                                chunk = json.loads(line)
                                token = chunk.get("response", "")
                                if token:
                                    yield token
                                if chunk.get("done", False):
                                    break
                            except Exception:
                                continue
        except Exception:
            from app.llm.expert_agent import expert_agent
            async for token in expert_agent.stream_expert_response(prompt):
                yield token

ollama_client = OllamaClient()
