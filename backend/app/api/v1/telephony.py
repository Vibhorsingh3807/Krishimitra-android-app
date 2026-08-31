from fastapi import APIRouter, Request, Form, Response, BackgroundTasks
from typing import Optional, List
from backend.app.services.telephony_service import telephony_service
from backend.app.schemas.telephony import (
    OutboundCallRequest,
    OutboundCallResponse,
    SendSMSRequest,
    SendSMSResponse,
    TelephonySessionInfo
)

router = APIRouter()

@router.post("/voice", summary="Twilio Inbound Voice Webhook")
async def inbound_voice_webhook(
    CallSid: str = Form(...),
    From: str = Form(...),
    To: Optional[str] = Form(None)
):
    """
    Primary Twilio webhook for incoming telephone calls.
    Returns TwiML greeting in Hindi with a speech gather prompt.
    """
    twiml_content = telephony_service.handle_inbound_call(
        call_sid=CallSid,
        from_phone=From
    )
    return Response(content=twiml_content, media_type="application/xml")


@router.post("/gather", summary="Twilio Speech Gather Webhook")
async def speech_gather_webhook(
    CallSid: str = Form(...),
    From: str = Form(...),
    SpeechResult: Optional[str] = Form(None),
    Confidence: Optional[float] = Form(None)
):
    """
    Receives transcribed speech from the farmer in Hindi/English,
    queries KrishiMitra's AI RAG engine, and speaks back the answer in Hindi.
    """
    twiml_content = await telephony_service.handle_speech_gather(
        call_sid=CallSid,
        from_phone=From,
        speech_result=SpeechResult
    )
    return Response(content=twiml_content, media_type="application/xml")


@router.post("/status", summary="Twilio Call Status Callback")
async def call_status_callback(
    background_tasks: BackgroundTasks,
    CallSid: str = Form(...),
    CallStatus: str = Form(...),
    CallDuration: Optional[str] = Form(None)
):
    """
    Called by Twilio when a call finishes (completed, busy, no-answer).
    Triggers automated SMS summary dispatch in the background.
    """
    background_tasks.add_task(
        telephony_service.handle_call_status,
        call_sid=CallSid,
        call_status=CallStatus,
        call_duration=CallDuration
    )
    return {"status": "success", "call_sid": CallSid, "call_status": CallStatus}


@router.post("/outbound-call", response_model=OutboundCallResponse, summary="Initiate Automated Outbound Advisory Call")
async def trigger_outbound_call(request: OutboundCallRequest):
    """
    Initiates an automated voice call to a farmer with a custom advisory/alert in Hindi.
    """
    return await telephony_service.initiate_outbound_call(
        to_phone=request.to_phone,
        message=request.message,
        language=request.language
    )


@router.post("/send-sms", response_model=SendSMSResponse, summary="Send Direct Agricultural SMS Advisory")
async def trigger_send_sms(request: SendSMSRequest):
    """
    Directly sends an SMS advisory in Hindi/English to a farmer's mobile phone.
    """
    return await telephony_service.send_sms(
        to_phone=request.to_phone,
        message=request.message
    )


@router.get("/sessions", response_model=List[TelephonySessionInfo], summary="List Telephony Voice Call Sessions")
async def list_telephony_sessions():
    """
    Returns active and past telephony sessions, queries asked by farmers, and SMS status.
    """
    return telephony_service.list_sessions()
