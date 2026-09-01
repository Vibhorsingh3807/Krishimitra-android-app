import asyncio
import re
from typing import AsyncGenerator, Dict, Any, List

class AgronomicExpertAgent:
    """
    Intelligent Agricultural Domain Expert Agent providing detailed, multi-paragraph,
    scientifically grounded ICAR recommendations for Indian farmers when local Ollama is offline.
    """

    def generate_expert_response(self, prompt: str, zone_context: Dict[str, Any] = None) -> str:
        p = prompt.lower()
        is_hindi = any('\u0900' <= c <= '\u097f' for c in prompt) or any(w in p for w in ["kya", "kaise", "kab", "kitna", "pani", "khat", "rog"])

        # 1. Zone 7 / Digital Twin Context Query
        if zone_context or ("ज़ोन" in p or "zone" in p or "twin" in p or "डिजिटल" in p):
            moisture = zone_context.get("moisture", 18) if zone_context else 18
            temp = zone_context.get("temperature", 36.0) if zone_context else 36.0
            zid = zone_context.get("id", "Z-07") if zone_context else "Z-07"
            
            if is_hindi:
                return (
                    f"🌾 **डिजिटल ट्विन ज़ोन {zid} स्थिति एवं विशेषज्ञ परामर्श:**\n\n"
                    f"• **मृदा नमी स्तर:** {moisture}% (गंभीर जल तनाव / Acute Water Stress)\n"
                    f"• **कैनोपी तापमान:** {temp}°C (गर्मी का दबाव / Heat Stress)\n"
                    f"• **स्थिति विश्लेषण:** ज़ोन {zid} में नमी 20% से काफी नीचे आ गई है। इस समय पौधों की जड़ों को पर्याप्त जल न मिलने से बढ़वार और टिलरिंग (कल्ले फूटने) पर विपरीत असर पड़ सकता है।\n\n"
                    f"**तत्काल अनुशंसित कदम:**\n"
                    f"1. **35 मिमी हल्की सिंचाई:** इस ज़ोन में तुरंत 35 मिमी सिंचाई करें। दोपहर की तेज धूप के बजाय शाम 4 बजे के बाद पानी लगाना सबसे लाभदायक होगा।\n"
                    f"2. **मल्चिंग / नमी संरक्षण:** नमी को बनाए रखने के लिए क्यारियों में पुआल या सूखी पत्तियों की हल्की मल्चिंग कर सकते हैं।\n"
                    f"3. **ट्विन में दर्ज करें:** सिंचाई करने के बाद नीचे **'✓ सिंचाई दर्ज करें (Mark Irrigated)'** बटन दबाएं ताकि डिजिटल ट्विन का नमी स्तर सुधरकर 32% (सामान्य) हो जाए।"
                )
            else:
                return (
                    f"🌾 **Field Digital Twin Zone {zid} Analysis & Advisory:**\n\n"
                    f"• **Soil Moisture:** {moisture}% (Critical Water Stress)\n"
                    f"• **Canopy Temperature:** {temp}°C (Heat Stress Detected)\n"
                    f"• **Diagnosis:** Zone {zid} moisture has dropped below the critical 20% threshold. Without intervention, root elongation and tillering vigor will decline.\n\n"
                    f"**Recommended Immediate Actions:**\n"
                    f"1. **Deliver 35mm Irrigation:** Irrigate Zone {zid} immediately. Evening irrigation is strongly recommended to reduce evaporative loss.\n"
                    f"2. **Soil Aeration:** Ensure drainage furrows are clear to prevent water stagnation once irrigated.\n"
                    f"3. **Update Digital Twin:** Click **'Mark Irrigated'** after watering so the living digital twin registers moisture restoration to 32% (Healthy)."
                )

        # 2. Wheat Irrigation / First Water (CRI Stage)
        if any(w in p for w in ["गेहूं", "wheat"]) and any(w in p for w in ["पानी", "irrigation", "सिंचाई", "water", "कब"]):
            if is_hindi:
                return (
                    "🌾 **गेहूं की फसल में सिंचाई का संपूर्ण ICAR शेड्यूल:**\n\n"
                    "1. **पहली सिंचाई (CRI - मुकुट जड़ अवस्था):**\n"
                    "   • **समय:** बुवाई के 20 से 25 दिन बाद। यह गेहूं की सबसे संवेदनशील अवस्था है। यदि इस समय पानी न दिया जाए तो कल्ले कम निकलते हैं और 25-30% उपज घट सकती है।\n"
                    "   • **सावधानी:** पहली सिंचाई हमेशा हल्की (30-35 मिमी) करें, भारी पानी लगाने से पौधे पीले पड़ सकते हैं।\n\n"
                    "2. **दूसरी सिंचाई (टिलरिंग अवस्था):**\n"
                    "   • बुवाई के 40-45 दिन बाद (कल्ले फूटते समय)।\n\n"
                    "3. **तीसरी सिंचाई (गांठ बनने पर):**\n"
                    "   • बुवाई के 60-65 दिन बाद।\n\n"
                    "4. **चौथी सिंचाई (फूल व बाली आने पर):**\n"
                    "   • बुवाई के 80-85 दिन बाद।\n\n"
                    "5. **पांचवीं सिंचाई (दूधिया अवस्था):**\n"
                    "   • बुवाई के 100-105 दिन बाद (दाना भरते समय)।\n\n"
                    "💡 **खाद टिप:** पहली और दूसरी सिंचाई के ठीक 2-3 दिन बाद प्रति एकड़ 35-40 किग्रा यूरिया का बुरकाव करें।"
                )
            else:
                return (
                    "🌾 **Complete ICAR Irrigation Schedule for Wheat:**\n\n"
                    "1. **1st Irrigation (Crown Root Initiation - CRI Stage):**\n"
                    "   • **Timing:** 20 to 25 days after sowing (DAS). This is the most critical stage. Skipping this water can reduce tillering and yield by 25-30%.\n"
                    "   • **Precaution:** Apply light irrigation (30-35mm). Over-flooding can turn seedlings yellow.\n\n"
                    "2. **2nd Irrigation (Tillering Stage):** 40-45 DAS.\n"
                    "3. **3rd Irrigation (Jointing Stage):** 60-65 DAS.\n"
                    "4. **4th Irrigation (Flowering/Booting Stage):** 80-85 DAS.\n"
                    "5. **5th Irrigation (Milking/Grain Filling Stage):** 100-105 DAS.\n\n"
                    "💡 **Fertilizer Tip:** Top-dress 35-40 kg Urea per acre 2-3 days after the 1st and 2nd irrigations when soil is moist."
                )

        # 3. Yellow Rust / Leaf Disease
        if any(w in p for w in ["पीला", "रतुआ", "rust", "रोग", "disease", "पत्ती", "fungus", "कीट"]):
            if is_hindi:
                return (
                    "🍃 **पीला रतुआ (Yellow Rust) एवं पत्ती रोगों का प्रमाणित उपचार:**\n\n"
                    "• **पहचान:** पत्तियों पर चमकीली पीली धारियां और पाउडर जैसे फफोले बनते हैं। छूने पर हाथ में हल्दी जैसा पीला चूर्ण लगता है।\n\n"
                    "**1. रासायनिक उपचार (तुरंत रोकथाम हेतु):**\n"
                    "   • **प्रोपिकोनाजोल 25% EC (टिल्ट / Tilt):** 1 मिली प्रति लीटर पानी (200 मिली प्रति 200 लीटर पानी प्रति एकड़)।\n"
                    "   • **अथवा टेबुकोनाजोल 25.9% EC:** 1.25 मिली प्रति लीटर पानी में मिलाकर साफ धूप वाले दिन छिड़काव करें।\n"
                    "   • यदि रोग का प्रकोप अधिक हो, तो 12-15 दिन बाद दोबारा छिड़काव करें।\n\n"
                    "**2. जैविक / प्राकृतिक उपचार:**\n"
                    "   • **नीम का तेल (10,000 PPM):** 3 मिली प्रति लीटर पानी में शैम्पू मिलाकर स्प्रे करें।\n"
                    "   • **ट्राइकोडर्मा विरिडी:** 5 ग्राम प्रति लीटर पानी का पर्णीय छिड़काव करें।\n\n"
                    "⚠️ **विशेष सावधानी:** रतुआ लगने पर खेत में अतिरिक्त यूरिया डालने से बचें, क्योंकि अधिक नाइट्रोजन फंगस के फैलाव को तेज करती है।"
                )
            else:
                return (
                    "🍃 **Management of Yellow Rust & Foliar Diseases:**\n\n"
                    "• **Symptoms:** Parallel yellow pustules along leaf veins that leave a turmeric-like powder on fingers.\n\n"
                    "**1. Chemical Control (Immediate Action):**\n"
                    "   • **Propiconazole 25% EC (Tilt):** 1 ml per litre of water (200 ml in 200 litres water per acre).\n"
                    "   • **Or Tebuconazole 25.9% EC:** 1.25 ml per litre of water sprayed during clear weather.\n"
                    "   • Repeat spray after 12-14 days if disease pressure persists.\n\n"
                    "**2. Organic & Bio-Control:**\n"
                    "   • **Neem Oil (10,000 PPM):** 3 ml per litre with a mild surfactant.\n"
                    "   • **Trichoderma viride:** 5 g per litre foliar spray.\n\n"
                    "⚠️ **Caution:** Do not apply excess Nitrogen (Urea), as high vegetative canopy humidity accelerates fungal sporulation."
                )

        # 4. Fertilizers & Urea / DAP
        if any(w in p for w in ["खाद", "यूरिया", "dap", "उर्वरक", "fertilizer", "npk", "dose", "मात्रा"]):
            if is_hindi:
                return (
                    "🧪 **संतुलित उर्वरक प्रबंधन (ICAR प्रमाणित NPK अनुपात):**\n\n"
                    "• **गेहूं हेतु मानक अनुपात:** 120 किग्रा नाइट्रोजन, 60 किग्रा फास्फोरस, 40 किग्रा पोटाश (प्रति हेक्टेयर)।\n\n"
                    "**प्रति एकड़ संतुलित मात्रा:**\n"
                    "1. **बुवाई के समय (बेसल डोज):**\n"
                    "   • DAP (18:46:0): 50 किग्रा (1 बोरी)\n"
                    "   • MOP (म्यूरेट ऑफ पोटाश): 20-25 किग्रा\n"
                    "   • जिंक सल्फेट (33%): 5 किग्रा (मिट्टी में मिलाएँ)\n\n"
                    "2. **पहली सिंचाई पर (टॉप ड्रेसिंग):**\n"
                    "   • यूरिया: 40-45 किग्रा प्रति एकड़।\n\n"
                    "3. **दूसरी सिंचाई पर (टॉप ड्रेसिंग):**\n"
                    "   • यूरिया: 40-45 किग्रा प्रति एकड़।\n\n"
                    "💡 **नैनो यूरिया विकल्प:** बालियां निकलते समय नैनो यूरिया (4 मिली/लीटर) और 0:52:34 का स्प्रे करने से दाने चमकदार और भारी बनते हैं।"
                )
            else:
                return (
                    "🧪 **Balanced Fertilizer Application (ICAR Verified Dosages):**\n\n"
                    "• **Recommended NPK Ratio:** 120:60:40 kg/hectare.\n\n"
                    "**Per Acre Recommended Dose:**\n"
                    "1. **Basal Dose (At Sowing):**\n"
                    "   • DAP (18:46:0): 50 kg (1 bag)\n"
                    "   • MOP (Potash): 20-25 kg\n"
                    "   • Zinc Sulphate (33%): 5 kg per acre\n\n"
                    "2. **At 1st Irrigation (CRI):** Top-dress 40-45 kg Urea.\n"
                    "3. **At 2nd Irrigation (Tillering):** Top-dress 40-45 kg Urea.\n\n"
                    "💡 **Foliar Nutrition:** Spray NPK 0:52:34 @ 1 kg in 100L water at booting stage for maximum grain filling and weight."
                )

        # 5. KCC & Loans & Schemes
        if any(w in p for w in ["kcc", "लोन", "loan", "योजना", "scheme", "pm kisan", "pm-kisan", "ब्याज", "credit"]):
            if is_hindi:
                return (
                    "💰 **किसान क्रेडिट कार्ड (KCC) एवं सरकारी योजनाओं की जानकारी:**\n\n"
                    "1. **किसान क्रेडिट कार्ड (KCC) फसल ऋण:**\n"
                    "   • **ऋण सीमा:** ₹1.60 लाख तक बिना किसी बंधक (Collateral) के, अधिकतम ₹3 लाख।\n"
                    "   • **प्रभावी ब्याज दर:** केवल **4% वार्षिक** (7% सामान्य दर में से समय पर चुकाने पर 3% की सरकारी छूट मिलती है)।\n"
                    "   • **आवश्यक दस्तावेज:** खतौनी/भू-अभिलेख नकल, आधार कार्ड, पैन कार्ड, बैंक पासबुक और फसल बुवाई प्रमाण पत्र।\n\n"
                    "2. **पीएम-किसान सम्मान निधि (PM-KISAN):**\n"
                    "   • सभी पात्र किसान परिवारों को प्रतिवर्ष ₹6,000 की वित्तीय सहायता 3 किस्तों में (₹2,000 प्रत्येक 4 माह में) सीधे बैंक खाते में मिलती है।\n"
                    "   • पोर्टल: `pmkisan.gov.in` (e-KYC अनिवार्य)।\n\n"
                    "3. **प्रधानमंत्री फसल बीमा योजना (PMFBY):**\n"
                    "   • रबी फसलों पर केवल 1.5% और खरीफ फसलों पर 2% प्रीमियम देकर सूखा, ओलावृष्टि व बाढ़ से फसल नुकसान की भरपाई पाएं।"
                )
            else:
                return (
                    "💰 **Kisan Credit Card (KCC) & Agricultural Financing:**\n\n"
                    "1. **Kisan Credit Card (KCC):**\n"
                    "   • **Credit Limit:** Up to ₹1.60 Lakh collateral-free; total limit up to ₹3 Lakh.\n"
                    "   • **Effective Interest Rate:** Only **4% per annum** (7% standard rate minus 3% prompt repayment incentive).\n"
                    "   • **Documents Required:** Land Record (Khatauni/7/12), Aadhaar Card, PAN Card, and Bank Passbook.\n\n"
                    "2. **PM-KISAN Scheme:**\n"
                    "   • ₹6,000/year in three equal installments of ₹2,000 transferred via DBT.\n"
                    "   • Official Portal: `pmkisan.gov.in`.\n\n"
                    "3. **Pradhan Mantri Fasal Bima Yojana (PMFBY):**\n"
                    "   • Comprehensive insurance covering non-preventable natural risks at a nominal 1.5% premium for Rabi crops and 2.0% for Kharif crops."
                )

        # 6. General Agricultural / Weather / Soil Guidance
        if is_hindi:
            return (
                f"🌾 **कृषिमित्र विशेषज्ञ कृषि परामर्श:**\n\n"
                f"आपके प्रश्न **'{prompt}'** के संबंध में भारतीय कृषि अनुसंधान परिषद (ICAR) का प्रामाणिक दिशा-निर्देश:\n\n"
                f"1. **मृदा व पोषण प्रबंधन:** किसी भी फसल में खाद डालने से पूर्व अपनी मिट्टी का सॉइल हेल्थ कार्ड जांचें। खेत में जीवांश कार्बन (Organic Carbon) बढ़ाने हेतु गोबर की सड़ी खाद या वर्मीकम्पोस्ट अवश्य डालें।\n"
                f"2. **सिंचाई एवं जल दक्षता:** मौसम पूर्वानुमान को ध्यान में रखकर ही पानी लगाएं। यदि आगामी 48 घंटों में बारिश की संभावना 50% से अधिक हो तो सिंचाई स्थगित करें।\n"
                f"3. **एकीकृत कीट प्रबंधन (IPM):** रासायनिक कीटनाशकों का अंधाधुंध छिड़काव न करें। पहले फेरोमोन ट्रैप, नीम अर्क और जैव-कवकनाशी का प्रयोग करें।\n\n"
                f"📌 *आप डिजिटल खेत ट्विन के किसी भी ज़ोन का स्वास्थ्य देखने हेतु ज़ोन मानचित्र का उपयोग कर सकते हैं।* "
            )
        else:
            return (
                f"🌾 **KrishiMitra Expert Agronomic Advisory:**\n\n"
                f"Regarding your query **'{prompt}'**, here is the ICAR-grounded guidance:\n\n"
                f"1. **Soil & Nutrient Management:** Base fertilizer application on Soil Health Card parameters. Maintain soil organic carbon by incorporating compost or green manure.\n"
                f"2. **Irrigation Efficiency:** Align irrigation schedules with atmospheric evapotranspiration. If rainfall probability exceeds 50% in the next 48 hours, postpone irrigation.\n"
                f"3. **Integrated Pest Management (IPM):** Prioritize biological controls (Neem, Trichoderma, light traps) before applying systemic chemical pesticides.\n\n"
                f"📌 *Use the Field Digital Twin tab to inspect real-time zone-wise soil moisture and vegetation vigor.*"
            )

    async def stream_expert_response(self, prompt: str, zone_context: Dict[str, Any] = None) -> AsyncGenerator[str, None]:
        """Streams the expert response token by token to simulate real-time AI generation"""
        full_text = self.generate_expert_response(prompt, zone_context)
        # Split into realistic tokens
        words = re.findall(r'\S+|\n', full_text)
        for w in words:
            if w == '\n':
                yield '\n'
            else:
                yield w + ' '
            await asyncio.sleep(0.018) # ~55 tokens per second for smooth natural streaming

expert_agent = AgronomicExpertAgent()
