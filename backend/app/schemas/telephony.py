from pydantic import BaseModel, Field
from typing import Optional, List

class TwilioVoiceWebhookForm(BaseModel):
    """Parameters sent by Twilio via application/x-www-form-urlencoded"""
    CallSid: str = Field(..., description="Unique Twilio Call ID")
    From: str = Field(..., description="Caller's phone number")
    To: str = Field(..., description="Twilio virtual number called")
    CallStatus: Optional[str] = Field(None, description="Current call status (ringing, in-progress, completed)")
    SpeechResult: Optional[str] = Field(None, description="Transcribed speech from farmer in Hindi/English")
    Confidence: Optional[float] = Field(None, description="Speech recognition confidence score (0.0 to 1.0)")
    Digits: Optional[str] = Field(None, description="DTMF keypad digits if pressed")
    FromCity: Optional[str] = None
    FromState: Optional[str] = None
    FromCountry: Optional[str] = None
    CallDuration: Optional[str] = None

class OutboundCallRequest(BaseModel):
    """Trigger an automated voice alert call to a farmer"""
    to_phone: str = Field(..., description="Farmer's phone number with country code (e.g. +919876543210)")
    message: str = Field(..., description="Message/advisory to speak to the farmer in Hindi")
    language: str = Field("hi-IN", description="Speech synthesis language code")
    alert_type: Optional[str] = Field("general", description="weather_alert, pest_warning, or scheme_reminder")

class SendSMSRequest(BaseModel):
    """Directly send agricultural SMS advisory to farmer"""
    to_phone: str = Field(..., description="Farmer's mobile phone number")
    message: str = Field(..., description="Text advisory to send in Hindi or English")

class TelephonySessionInfo(BaseModel):
    call_sid: str
    caller_phone: str
    start_time: str
    status: str
    queries_count: int
    queries: List[str]
    last_topic: Optional[str] = None
    sms_sent: bool = False

class OutboundCallResponse(BaseModel):
    call_sid: Optional[str] = None
    status: str
    message: str

class SendSMSResponse(BaseModel):
    message_sid: Optional[str] = None
    status: str
    to: str
    message: str
