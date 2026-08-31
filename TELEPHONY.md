# TELEPHONY.md — AI Kisan Phone Helpline (Twilio Voice & SMS)

KrishiMitra includes an enterprise-grade **AI Kisan Phone Helpline** that enables any farmer to dial a standard phone number from **any basic 2G feature phone or mobile** (no internet, no app required). 

When a farmer calls:
1. Twilio Voice connects the call and plays a welcoming greeting in conversational Hindi using Amazon Polly's natural Indian voice (`Aditi`).
2. The farmer speaks their farming question in Hindi (*"गेहूं में कौन सी खाद डालें?"* or *"धान में पत्तियां पीली पड़ रही हैं"*).
3. Twilio transcribes the speech (`hi-IN`) and posts it to KrishiMitra's FastAPI backend.
4. KrishiMitra's **Server-Side AI RAG Agent** (powered by ICAR scientific data + Gemini / OpenAI / Local NLP) retrieves verified agronomic advice and responds in warm, clear Hindi.
5. The AI speaks the answer directly over the cellular phone call.
6. The farmer can ask follow-up questions in a multi-turn conversation loop.
7. Upon hanging up, an automated **SMS summary** of the advice is dispatched to the farmer's mobile number.

---

## 📞 Architecture Diagram

```
+-----------------------------------------------------------------------------------+
|                           FARMER (ANY 2G/4G PHONE)                                |
|  • Dial Virtual Helpline Number                                                  |
|  • Speak query in Hindi: "धान में कौन सी खाद डालें?"                              |
|  • Listen to AI spoken answer over cellular voice call                            |
|  • Receive follow-up SMS advisory on mobile phone                                 |
+-----------------------------------------┬-----------------------------------------+
                                          │ Cellular Voice Call
                                          ▼
+-----------------------------------------------------------------------------------+
|                                 TWILIO VOICE GATEWAY                              |
|  • Inbound Call Webhook -> POST /api/v1/telephony/voice                           |
|  • Speech Recognition -> <Gather input="speech" language="hi-IN">                 |
|  • Text-to-Speech Engine -> <Say language="hi-IN" voice="Polly.Aditi">            |
|  • Call Status Callback -> POST /api/v1/telephony/status                          |
|  • Programmable SMS API -> POST /2010-04-01/Accounts/{SID}/Messages.json        |
+-----------------------------------------┬-----------------------------------------+
                                          │ Webhook (HTTPS via ngrok / Cloud Host)
                                          ▼
+-----------------------------------------------------------------------------------+
|                        KRISHIMITRA FASTAPI BACKEND SERVICE                        |
|                                                                                   |
|  +-----------------------------------------------------------------------------+  |
|  |                     Telephony Service (telephony_service.py)                |  |
|  |  • Multi-turn Session Manager (CallSid tracking)                            |  |
|  |  • TwiML XML Synthesizer                                                    |  |
|  |  • SMS Advisory Dispatcher (via httpx)                                      |  |
|  +-----------------------------------------------------------------------------+  |
|                                         │                                         |
|                                         ▼                                         |
|  +-----------------------------------------------------------------------------+  |
|  |                     Supercharged Cloud RAG + LLM Agent                      |  |
|  |  • 25 ICAR Crop Agronomic Guidelines                                        |  |
|  |  • 12 Plant Diseases (Symptoms, Organic & Chemical Dosages)                 |  |
|  |  • 8 Central Government Schemes (PM-KISAN, PMFBY, KCC)                      |  |
|  |  • Live Agricultural Weather API (Open-Meteo)                               |  |
|  |  • High-Capacity Cloud LLM (Gemini 1.5 Flash / OpenAI GPT-4o-mini)          |  |
|  +-----------------------------------------------------------------------------+  |
+-----------------------------------------------------------------------------------+
```

---

## 🚀 How to Set Up Your Free Prototype in 5 Minutes

### Step 1: Create a Free Twilio Trial Account
1. Visit **[https://www.twilio.com/try-twilio](https://www.twilio.com/try-twilio)** and sign up for a free account (no credit card required).
2. Verify your email and personal phone number.
3. On the welcome screen, choose:
   * **Which product are you using?** $\rightarrow$ *Voice*
   * **What are you building?** $\rightarrow$ *IVR / AI Voice Assistant*
   * **What language?** $\rightarrow$ *Python*

### Step 2: Get a Free Virtual Phone Number
1. From your Twilio Console Dashboard, click **"Get a trial phone number"** (or go to **Develop $\rightarrow$ Phone Numbers $\rightarrow$ Manage $\rightarrow$ Active Numbers**).
2. Twilio will assign you a free phone number (e.g. `+1 205 555 0199` or a local number).
3. Note down this number.

### Step 3: Copy Your Account SID and Auth Token
1. On the main **Twilio Console Dashboard** (`https://console.twilio.com`), look under **Account Info**:
   * **Account SID**: Starts with `AC...` (e.g. `AC1234567890abcdef1234567890abcdef`)
   * **Auth Token**: Click "Show" to copy your secret token.
2. In your trial account, you can call or receive calls to any **Verified Caller ID** (your personal phone number is automatically verified during signup). To add another number, go to **Phone Numbers $\rightarrow$ Manage $\rightarrow$ Verified Caller IDs**.

### Step 4: Configure Your Backend `.env`
In `Krishimitra-android-app/backend/.env` (or create from `.env.example`):
```env
# Twilio Telephony Credentials
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=your_auth_token_here
TWILIO_PHONE_NUMBER=+1xxxxxxxxxx

# Optional: Add Gemini or OpenAI for supercharged online LLM
AI_FALLBACK_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key_here

# Telephony Language & Voice
TELEPHONY_DEFAULT_LANGUAGE=hi-IN
TELEPHONY_TTS_VOICE=Polly.Aditi
TELEPHONY_SEND_SMS_SUMMARY=True
```

---

## 🌐 Exposing Your Local Backend with Ngrok

Because Twilio makes HTTP requests to your server when someone dials your number, your local `http://localhost:8000` must be accessible from the internet.

### 1. Install & Start Ngrok
If you do not have ngrok installed:
* Download from [https://ngrok.com/download](https://ngrok.com/download) or run `winget install ngrok`.
* Start an HTTP tunnel to port 8000:
```powershell
ngrok http 8000
```
Ngrok will display a public forwarding URL such as:
```
Forwarding: https://ab12-34-56-78-90.ngrok-free.app -> http://localhost:8000
```

### 2. Configure the Webhook in Twilio Console
1. Go to **Twilio Console $\rightarrow$ Phone Numbers $\rightarrow$ Manage $\rightarrow$ Active Numbers**.
2. Click on your active trial phone number.
3. Scroll down to the **Voice Configuration** section:
   * **A CALL COMES IN**: Select **Webhook**
   * **URL**: Enter your ngrok public URL with `/api/v1/telephony/voice`:
     ```
     https://ab12-34-56-78-90.ngrok-free.app/api/v1/telephony/voice
     ```
   * **HTTP Method**: Select **`HTTP POST`**
4. Under **Call Status Changes**:
   * **Status Callback URL**:
     ```
     https://ab12-34-56-78-90.ngrok-free.app/api/v1/telephony/status
     ```
   * **HTTP Method**: Select **`HTTP POST`**
5. Click **Save Configuration** at the bottom.

---

## 🎧 Testing Your Live AI Phone Helpline

1. Start the KrishiMitra backend:
   ```powershell
   cd "Krishimitra-android-app/backend"
   python -m uvicorn backend.app.main:app --port 8000 --reload
   ```
2. Dial your Twilio virtual number from your phone.
3. **Listen**: You will hear the welcoming Hindi greeting:
   > *"नमस्ते! कृषिमित्र किसान हेल्पलाइन में आपका स्वागत है। मैं आपका एआई कृषि साथी हूँ। आप अपनी फसल, खाद, सिंचाई, कीट रोग या सरकारी योजनाओं के बारे में कोई भी सवाल पूछ सकते हैं। कृपया बीप के बाद अपनी फसल या समस्या बताएं।"*
4. **Speak**: Speak naturally in Hindi:
   > *"धान में कौन सी खाद डालें?"*
5. **Receive Spoken Advice**: The AI speaks the ICAR verified fertilizer dosages and asks if you have any more questions.
6. **Ask Follow-up**: Speak another question (e.g. *"मौसम कैसा रहेगा?"* or *"पीएम किसान योजना क्या है?"*).
7. **Hang up**: Check your phone's SMS inbox—you will receive an automated text advisory summarizing the advice!

---

## 🛠️ Telephony API Endpoints Reference

| Endpoint | Method | Purpose | Input / Description |
| :--- | :---: | :--- | :--- |
| `/api/v1/telephony/voice` | `POST` | Inbound call webhook | Twilio form: `CallSid`, `From`, `To`. Returns TwiML XML with Hindi greeting. |
| `/api/v1/telephony/gather` | `POST` | Speech gather handler | Twilio form: `SpeechResult`, `Confidence`. Invokes AI & returns spoken TwiML. |
| `/api/v1/telephony/status` | `POST` | Call status callback | Triggered on call hangup. Automatically sends SMS summary to caller. |
| `/api/v1/telephony/outbound-call` | `POST` | Trigger automated outbound call | JSON: `{"to_phone": "+91...", "message": "...", "alert_type": "weather_alert"}` |
| `/api/v1/telephony/send-sms` | `POST` | Direct SMS dispatch | JSON: `{"to_phone": "+91...", "message": "..."}` |
| `/api/v1/telephony/sessions` | `GET` | Telephony session logs | Returns active/past calls, caller numbers, queries asked, and SMS status. |
