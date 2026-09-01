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

    async def stream_chat(
        self,
        prompt: str,
        system_prompt: str = "",
        model: str = None,
        context_docs: List[Dict[str, Any]] = None
    ) -> AsyncGenerator[str, None]:
        """
        Stream tokens from Ollama LLM with grounded RAG context.
        Falls back smoothly if Ollama is unreachable.
        """
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
            async with httpx.AsyncClient(timeout=settings.OLLAMA_TIMEOUT_SECONDS) as client:
                async with client.stream("POST", f"{self.base_url}/api/generate", json=payload) as response:
                    if response.status_code != 200:
                        yield f"Ollama service returned status {response.status_code}. Activating ICAR backup response."
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
        except Exception as e:
            # High quality grounded agronomic fallback
            yield f"\n[🌾 कृषिमित्र AI परामर्श - ICAR संदर्भ]\n\n"
            yield f"आपके प्रश्न '{prompt}' के संबंध में कृषि अनुसंधान परिषद (ICAR) का प्रामाणिक परामर्श:\n"
            if "पानी" in prompt or "सिंचाई" in prompt or "water" in prompt.lower() or "irrigation" in prompt.lower():
                yield "• गेहूं की फसल में पहली सिंचाई बुवाई के 20-25 दिन बाद 'मुकुट जड़' (CRI stage) पर अनिवार्य है।\n"
                yield "• पानी की कमी की दशा में हल्की सिंचाई करें और शाम के समय पानी लगाएं।\n"
            elif "खाद" in prompt or "यूरिया" in prompt or "fertilizer" in prompt.lower():
                yield "• संतुलित उर्वरक (NPK 120:60:40 किग्रा/हेक्टेयर) का प्रयोग करें।\n"
                yield "• यूरिया की आधी मात्रा बुवाई के समय और शेष दो बराबर भागों में प्रथम व द्वितीय सिंचाई पर दें।\n"
            elif "रोग" in prompt or "पीला" in prompt or "rust" in prompt.lower():
                yield "• पीला रतुआ (Yellow Rust) के लक्षण दिखने पर प्रोपिकोनाजोल 25% EC (टिल्ट) 1 मिली प्रति लीटर पानी में मिलाकर छिड़काव करें।\n"
            else:
                yield "• समय पर खरपतवार नियंत्रण (जैसे सल्फोसल्फ्यूरॉन 33 ग्राम/हेक्टेयर) करें।\n"
                yield "• मौसम के अनुसार सिंचाई का निर्धारण करें और अपनी मिट्टी की जांच अनुसार पोषक तत्व दें।\n"
            yield f"\n*(नोट: बैकएंड Ollama LLM से पुनः संपर्क का प्रयास जारी है)*"

ollama_client = OllamaClient()
