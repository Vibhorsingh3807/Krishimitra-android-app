import pytest
from fastapi.testclient import TestClient
from backend.app.main import app

client = TestClient(app)

def test_inbound_voice_webhook():
    """Test that incoming phone call returns valid TwiML greeting in Hindi"""
    payload = {
        "CallSid": "CA_TEST_001",
        "From": "+919876543210",
        "To": "+12055550199"
    }
    response = client.post("/api/v1/telephony/voice", data=payload)
    assert response.status_code == 200
    assert "application/xml" in response.headers["content-type"]
    
    xml = response.text
    assert "<Response>" in xml
    assert "<Say" in xml
    assert "hi-IN" in xml
    assert "कृषिमित्र" in xml
    assert "<Gather" in xml
    assert "/api/v1/telephony/gather" in xml


def test_speech_gather_hindi_wheat():
    """Test that speech gather in Hindi invokes RAG/AI and speaks advice"""
    payload = {
        "CallSid": "CA_TEST_001",
        "From": "+919876543210",
        "SpeechResult": "गेहूं में खाद कब और कितनी डालनी चाहिए",
        "Confidence": "0.92"
    }
    response = client.post("/api/v1/telephony/gather", data=payload)
    assert response.status_code == 200
    assert "application/xml" in response.headers["content-type"]
    
    xml = response.text
    assert "<Response>" in xml
    assert "<Say" in xml
    assert "hi-IN" in xml
    # Check that agricultural advice is spoken
    assert ("एनपीके" in xml or "खाद" in xml or "यूरिया" in xml or "उर्वरक" in xml)
    # Check that it asks if caller has another question (multi-turn)
    assert "<Gather" in xml


def test_speech_gather_empty_reprompt():
    """Test that empty or inaudible speech re-prompts the farmer politely"""
    payload = {
        "CallSid": "CA_TEST_001",
        "From": "+919876543210",
        "SpeechResult": "",
        "Confidence": "0.0"
    }
    response = client.post("/api/v1/telephony/gather", data=payload)
    assert response.status_code == 200
    xml = response.text
    assert "सुनाई नहीं दी" in xml or "स्पष्ट सुनाई नहीं दिया" in xml
    assert "<Gather" in xml


def test_call_status_callback():
    """Test call completion webhook and background task triggering"""
    payload = {
        "CallSid": "CA_TEST_001",
        "CallStatus": "completed",
        "CallDuration": "58"
    }
    response = client.post("/api/v1/telephony/status", data=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "success"
    assert data["call_sid"] == "CA_TEST_001"


def test_send_sms_advisory():
    """Test agricultural SMS advisory dispatch"""
    payload = {
        "to_phone": "+919876543210",
        "message": "कृषिमित्र सलाह: गेहूं में पहली सिंचाई क्राउन रूट इनीशिएशन (CRI) अवस्था पर 20-25 दिन बाद करें।"
    }
    response = client.post("/api/v1/telephony/send-sms", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["to"] == "+919876543210"
    assert data["status"] in ["sent", "simulated_queued"]


def test_outbound_call_advisory():
    """Test automated outbound alert call"""
    payload = {
        "to_phone": "+919876543210",
        "message": "मौसम चेतावनी! कल आपके क्षेत्र में तेज आंधी और बारिश की संभावना है। कृपया कटी हुई फसल सुरक्षित स्थान पर रखें।",
        "language": "hi-IN",
        "alert_type": "weather_alert"
    }
    response = client.post("/api/v1/telephony/outbound-call", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["status"] in ["queued", "simulated_initiated"]


def test_list_telephony_sessions():
    """Test fetching telephony sessions and query history"""
    response = client.get("/api/v1/telephony/sessions")
    assert response.status_code == 200
    sessions = response.json()
    assert isinstance(sessions, list)
    assert len(sessions) >= 1
    session = sessions[0]
    assert "call_sid" in session
    assert "caller_phone" in session
    assert "queries" in session
