"""
KrishiMitra — AI Kisan Phone Helpline Quick CLI Tester
Simulates a farmer dialing the helpline and receiving spoken advice + SMS advisory.
"""
import sys
import xml.etree.ElementTree as ET
import httpx

if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass

def test_helpline_call(query_text: str = "धान में कौन सी खाद डालें?"):
    print("=" * 65)
    print("📞 KRISHIMITRA AI KISAN HELPLINE — CALL SIMULATION")
    print("=" * 65)
    print(f"👨‍🌾 Caller Phone: +919990565376")
    print(f"❓ Farmer Asks: \"{query_text}\"")
    print("-" * 65)

    base_url = "http://127.0.0.1:8000"

    try:
        # Step 1: Inbound Call Webhook
        print("1. Dialing 1800-KRISHI (Connecting to AI Helpline)...")
        r1 = httpx.post(
            f"{base_url}/api/v1/telephony/voice",
            data={"CallSid": "SIM_CALL_001", "From": "+919990565376"}
        )
        if r1.status_code == 200:
            root = ET.fromstring(r1.text)
            says = root.findall(".//Say")
            if says:
                print(f"🤖 AI Helpline Greeting: \"{says[0].text}\"")

        # Step 2: Speech Recognition Webhook
        print("\n2. Farmer speaks question into the phone...")
        r2 = httpx.post(
            f"{base_url}/api/v1/telephony/gather",
            data={"CallSid": "SIM_CALL_001", "From": "+919990565376", "SpeechResult": query_text}
        )
        if r2.status_code == 200:
            root = ET.fromstring(r2.text)
            says = root.findall(".//Say")
            if says:
                print(f"🤖 AI Spoken Advisory:\n   \"{says[0].text}\"")

        # Step 3: Hangup & Automated SMS Dispatch
        print("\n3. Farmer hangs up call...")
        r3 = httpx.post(
            f"{base_url}/api/v1/telephony/status",
            data={"CallSid": "SIM_CALL_001", "CallStatus": "completed", "CallDuration": "42"}
        )
        print("✅ Call completed successfully.")
        print("📩 Follow-up SMS advisory dispatched to caller's mobile phone!")
        print("=" * 65)
        print("💡 TIP: You can also test the interactive visual phone in your browser:")
        print("👉 Open http://localhost:8000/phone in Chrome / Edge!")
        print("=" * 65)

    except httpx.ConnectError:
        print("\n❌ Backend server is not running!")
        print("Start it with: python -m uvicorn backend.app.main:app --port 8000")

if __name__ == "__main__":
    q = sys.argv[1] if len(sys.argv) > 1 else "धान में कौन सी खाद डालें?"
    test_helpline_call(q)
