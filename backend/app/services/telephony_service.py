import datetime
import os
import xml.sax.saxutils as xml_escape
from typing import Dict, Any, Optional, List
import httpx

from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.app.services.ai_provider import get_ai_provider
from backend.app.schemas.telephony import (
    TelephonySessionInfo,
    OutboundCallResponse,
    SendSMSResponse
)

class TelephonyService:
    """
    Supercharged Server-Side Voice Agent & Telephony Service.
    Connects Twilio Voice/SMS with KrishiMitra's ICAR grounded AI.
    """

    def __init__(self):
        # In-memory session store: CallSid -> session state dict
        self._sessions: Dict[str, Dict[str, Any]] = {}

    def get_session(self, call_sid: str) -> Optional[Dict[str, Any]]:
        return self._sessions.get(call_sid)

    def list_sessions(self) -> List[TelephonySessionInfo]:
        results = []
        for sid, data in self._sessions.items():
            results.append(
                TelephonySessionInfo(
                    call_sid=sid,
                    caller_phone=data.get("caller_phone", "Unknown"),
                    start_time=data.get("start_time", ""),
                    status=data.get("status", "active"),
                    queries_count=len(data.get("queries", [])),
                    queries=data.get("queries", []),
                    last_topic=data.get("last_topic"),
                    sms_sent=data.get("sms_sent", False)
                )
            )
        return results

    def build_twiml(self, say_text: str, gather_action: Optional[str] = None, prompt_next: Optional[str] = None, language: Optional[str] = None) -> str:
        """
        Builds standard compliant TwiML XML for Twilio Voice response.
        """
        lang = language or settings.TELEPHONY_DEFAULT_LANGUAGE
        voice = settings.TELEPHONY_TTS_VOICE
        safe_say = xml_escape.escape(say_text.strip())

        twiml = ['<?xml version="1.0" encoding="UTF-8"?>', '<Response>']
        twiml.append(f'    <Say language="{lang}" voice="{voice}">{safe_say}</Say>')

        if gather_action:
            action_url = gather_action
            if not action_url.startswith("http") and settings.BASE_WEBHOOK_URL:
                action_url = f"{settings.BASE_WEBHOOK_URL.rstrip('/')}{action_url}"

            twiml.append(f'    <Gather input="speech" language="{lang}" speechTimeout="{settings.TELEPHONY_SPEECH_TIMEOUT}" action="{action_url}" method="POST">')
            if prompt_next:
                safe_prompt = xml_escape.escape(prompt_next.strip())
                twiml.append(f'        <Say language="{lang}" voice="{voice}">{safe_prompt}</Say>')
            twiml.append('    </Gather>')
            twiml.append(f'    <Say language="{lang}" voice="{voice}">हमें आपकी आवाज सुनाई नहीं दी। कृषिमित्र हेल्पलाइन पर कॉल करने के लिए धन्यवाद।</Say>')
            twiml.append('    <Hangup/>')
        else:
            twiml.append('    <Hangup/>')

        twiml.append('</Response>')
        return "\n".join(twiml)

    def handle_inbound_call(self, call_sid: str, from_phone: str) -> str:
        """
        Generates initial welcome greeting and prompts the farmer to ask a question.
        """
        now = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
        self._sessions[call_sid] = {
            "call_sid": call_sid,
            "caller_phone": from_phone,
            "start_time": now,
            "status": "in-progress",
            "queries": [],
            "answers": [],
            "last_topic": None,
            "sms_sent": False
        }

        greeting = (
            "नमस्ते! कृषिमित्र किसान हेल्पलाइन में आपका स्वागत है। "
            "मैं आपका एआई कृषि साथी हूँ। "
            "आप अपनी फसल, खाद, सिंचाई, कीट रोग या सरकारी योजनाओं के बारे में कोई भी सवाल पूछ सकते हैं।"
        )
        gather_prompt = "कृपया बीप के बाद अपनी फसल या समस्या बताएं।"

        return self.build_twiml(
            say_text=greeting,
            gather_action="/api/v1/telephony/gather",
            prompt_next=gather_prompt
        )

    async def handle_speech_gather(self, call_sid: str, from_phone: str, speech_result: Optional[str]) -> str:
        """
        Processes the farmer's spoken query with the server-side RAG + LLM agent.
        """
        session = self._sessions.setdefault(call_sid, {
            "call_sid": call_sid,
            "caller_phone": from_phone,
            "start_time": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC"),
            "status": "in-progress",
            "queries": [],
            "answers": [],
            "last_topic": None,
            "sms_sent": False
        })

        if not speech_result or not speech_result.strip():
            retry_msg = "हमें आपका सवाल स्पष्ट सुनाई नहीं दिया।"
            return self.build_twiml(
                say_text=retry_msg,
                gather_action="/api/v1/telephony/gather",
                prompt_next="कृपया अपना सवाल दोबारा बोलें।"
            )

        clean_query = speech_result.strip()
        session["queries"].append(clean_query)
        logger.info(f"Telephony [{call_sid}] from {from_phone} asked: {clean_query}")

        # Query Supercharged Server-Side AI Agent
        ai_provider = get_ai_provider()
        ai_resp = await ai_provider.answer_query(
            query=clean_query,
            language="hi",
            context={"caller_phone": from_phone, "channel": "telephony_voice"}
        )

        answer_text = ai_resp.answer.strip()
        session["answers"].append(answer_text)
        session["last_topic"] = ai_resp.detected_intent

        # Format spoken audio for telephone clarity:
        # Keep speech conversational, polite, and reassuring
        spoken_response = (
            f"{answer_text} "
        )

        followup_prompt = "क्या आप कोई और सवाल पूछना चाहते हैं? अपना सवाल बोलें, या कॉल समाप्त करने के लिए फोन काट दें।"

        return self.build_twiml(
            say_text=spoken_response,
            gather_action="/api/v1/telephony/gather",
            prompt_next=followup_prompt
        )

    async def handle_call_status(self, call_sid: str, call_status: str, call_duration: Optional[str]) -> None:
        """
        Handles call termination and triggers automated SMS summary if enabled.
        """
        session = self._sessions.get(call_sid)
        if not session:
            return

        session["status"] = call_status
        session["duration"] = call_duration

        # Send SMS summary of advice to farmer's mobile number on call completion
        if call_status in ["completed", "answered"] and settings.TELEPHONY_SEND_SMS_SUMMARY and not session.get("sms_sent"):
            caller_phone = session.get("caller_phone")
            queries = session.get("queries", [])
            answers = session.get("answers", [])

            if caller_phone and answers:
                last_q = queries[-1] if queries else "कृषि सलाह"
                last_a = answers[-1]

                # Compose clean, concise Hindi SMS
                sms_body = (
                    f"🌾 कृषिमित्र किसान हेल्पलाइन परामर्श\n"
                    f"प्रश्न: {last_q[:60]}\n"
                    f"सलाह: {last_a[:250]}...\n\n"
                    f"अधिक जानकारी व सहायता हेतु किसान कॉल सेंटर: 1800-180-1551"
                )

                try:
                    await self.send_sms(to_phone=caller_phone, message=sms_body)
                    session["sms_sent"] = True
                    logger.info(f"Telephony SMS advisory sent to {caller_phone}")
                except Exception as e:
                    logger.error(f"Failed to dispatch telephony SMS: {e}")

    async def send_sms(self, to_phone: str, message: str) -> SendSMSResponse:
        """
        Sends an SMS message to a farmer's mobile phone via Twilio Messages API.
        Works in simulation mode if credentials are not configured.
        """
        if not settings.TWILIO_ACCOUNT_SID or not settings.TWILIO_AUTH_TOKEN:
            logger.info(f"[SIMULATION] Twilio credentials not configured. Mock SMS to {to_phone}: {message[:60]}...")
            return SendSMSResponse(
                message_sid="SIMULATED_MSG_" + datetime.datetime.now().strftime("%Y%m%d%H%M%S"),
                status="simulated_queued",
                to=to_phone,
                message="SMS simulated (Twilio credentials not configured in .env)"
            )

        url = f"https://api.twilio.com/2010-04-01/Accounts/{settings.TWILIO_ACCOUNT_SID}/Messages.json"
        auth = (settings.TWILIO_ACCOUNT_SID, settings.TWILIO_AUTH_TOKEN)
        payload = {
            "From": settings.TWILIO_PHONE_NUMBER,
            "To": to_phone,
            "Body": message
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, data=payload, auth=auth)
            if resp.status_code in [200, 201]:
                data = resp.json()
                return SendSMSResponse(
                    message_sid=data.get("sid"),
                    status=data.get("status", "sent"),
                    to=to_phone,
                    message="SMS sent successfully via Twilio"
                )
            else:
                logger.error(f"Twilio SMS error ({resp.status_code}): {resp.text}")
                return SendSMSResponse(
                    message_sid=None,
                    status="failed",
                    to=to_phone,
                    message=f"Twilio error: {resp.status_code} {resp.text}"
                )

    async def initiate_outbound_call(self, to_phone: str, message: str, language: str = "hi-IN") -> OutboundCallResponse:
        """
        Initiates an automated outbound call to a farmer (e.g. for frost/rain alert).
        """
        if not settings.TWILIO_ACCOUNT_SID or not settings.TWILIO_AUTH_TOKEN:
            logger.info(f"[SIMULATION] Outbound call to {to_phone}: {message[:60]}...")
            return OutboundCallResponse(
                call_sid="SIMULATED_CALL_" + datetime.datetime.now().strftime("%Y%m%d%H%M%S"),
                status="simulated_initiated",
                message="Call simulated (Twilio credentials not configured in .env)"
            )

        url = f"https://api.twilio.com/2010-04-01/Accounts/{settings.TWILIO_ACCOUNT_SID}/Calls.json"
        auth = (settings.TWILIO_ACCOUNT_SID, settings.TWILIO_AUTH_TOKEN)

        twiml = self.build_twiml(say_text=message, language=language)
        payload = {
            "From": settings.TWILIO_PHONE_NUMBER,
            "To": to_phone,
            "Twiml": twiml
        }

        async with httpx.AsyncClient(timeout=10.0) as client:
            resp = await client.post(url, data=payload, auth=auth)
            if resp.status_code in [200, 201]:
                data = resp.json()
                return OutboundCallResponse(
                    call_sid=data.get("sid"),
                    status=data.get("status", "queued"),
                    message="Outbound call queued successfully via Twilio"
                )
            else:
                logger.error(f"Twilio Outbound call error ({resp.status_code}): {resp.text}")
                return OutboundCallResponse(
                    call_sid=None,
                    status="failed",
                    message=f"Twilio error: {resp.status_code} {resp.text}"
                )

telephony_service = TelephonyService()
