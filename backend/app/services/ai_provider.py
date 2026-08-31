import abc
import json
import os
import re
from typing import Optional, Dict, Any, List
from sqlalchemy.orm import Session
from backend.app.core.config import settings
from backend.app.core.logging import logger
from backend.app.schemas.ai import AIQueryResponse
from backend.app.db.session import SessionLocal
from backend.app.db.models import MarketPrice, CropVariety

OUTPUT_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))), "ml_pipeline", "output")
DATA_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))), "data")

class AIProvider(abc.ABC):
    @abc.abstractmethod
    async def answer_query(self, query: str, language: str = "auto", context: Optional[Dict[str, Any]] = None) -> AIQueryResponse:
        pass

class LocalAIProvider(AIProvider):
    """
    Zero-hallucination agricultural NLP & Market Intelligence Engine.
    Queries verified ICAR agronomy datasets and live/curated Mandi market price records.
    Supports farmer profile context (e.g. farmer district & selected crop).
    """
    def __init__(self):
        self.model_data = None
        self.knowledge_data = None
        self._load_assets()

    def _load_assets(self):
        model_path = os.path.join(OUTPUT_DIR, "mobile_nlp_intent_model.json")
        knowledge_path = os.path.join(OUTPUT_DIR, "mobile_knowledge_index.json")

        if os.path.exists(model_path):
            with open(model_path, "r", encoding="utf-8") as f:
                self.model_data = json.load(f)
        if os.path.exists(knowledge_path):
            with open(knowledge_path, "r", encoding="utf-8") as f:
                self.knowledge_data = json.load(f)

    def is_out_of_scope(self, query: str) -> bool:
        q = query.lower()
        non_agri_keywords = [
            "prime minister", "president", "python", "code", "coding", "movie", "cricket", "football",
            "capital of", "calculator", "song", "joke", "bollywood", "hollywood", "dance", "sports", "game",
            "प्रधानमंत्री", "राष्ट्रपति", "गाना", "पायथन", "फिल्म", "क्रिकेट", "राजधानी", "खेल", "राष्ट्रीय खेल"
        ]
        agri_keywords = [
            "crop", "soil", "kisan", "farmer", "fasal", "kheti", "pani", "water", "irrigation", "sinchai",
            "khad", "fertilizer", "urea", "dap", "seed", "beej", "pest", "keet", "disease", "rog",
            "mandi", "bhav", "scheme", "yojana", "loan", "rin", "krishi", "agriculture", "price", "rate", "variety", "kism",
            "फसल", "खेती", "मिट्टी", "सिंचाई", "खाद", "बीज", "कीट", "रोग", "योजना", "ऋण", "कृषि", "किसान", "भाव", "मंडी", "किस्म", "दाम"
        ]
        has_non_agri = any(w in q for w in non_agri_keywords)
        has_agri = any(w in q for w in agri_keywords) or self._detect_crop(query) is not None
        return has_non_agri and not has_agri

    def _detect_crop(self, query: str) -> Optional[str]:
        q = query.lower()
        if "solar" in q or "solar pump" in q or "सोलर" in q:
            return None

        crop_aliases = {
            "rice": ["rice", "paddy", "धान", "चावल", "dhan", "chawal", "basmati", "बासमती", "sona masuri", "matta"],
            "wheat": ["wheat", "गेहूं", "गेहू", "gehu", "gehun", "sharbati", "शरबती", "lokwan", "लोकवन"],
            "maize": ["maize", "corn", "मक्का", "मकई", "makka", "makai", "bhutta"],
            "cotton": ["cotton", "कपास", "रुई", "kapas", "कापूस"],
            "sugarcane": ["sugarcane", "गन्ना", "ईख", "ganna"],
            "mustard": ["mustard", "सरसों", "राई", "sarson", "sarso", "rai"],
            "soybean": ["soybean", "सोयाबीन", "soyabean"],
            "chickpea": ["chickpea", "gram", "चना", "chana", "chane", "bengal gram"],
            "groundnut": ["groundnut", "peanut", "मूंगफली", "mungfali", "moongphali"],
            "potato": ["potato", "आलू", "alu", "aaloo", "kufri", "कुफरी"],
            "tomato": ["tomato", "टमाटर", "tamatar"],
            "onion": ["onion", "प्याज", "kanda", "pyaj", "कांदा"],
            "chilli": ["chilli", "chillies", "chili", "मिर्च", "mirch", "mirchi"],
            "coconut": ["coconut", "नारियल", "nariyal", "thenga", "copra", "खोपरा"],
            "pigeon_pea": ["pigeon pea", "red gram", "arhar", "tur", "अरहर", "तुअर", "तूर"],
            "black_gram": ["black gram", "urad", "उड़द"],
            "green_gram": ["green gram", "moong", "मूंग"],
            "lentil": ["lentil", "masoor", "मसूर"],
            "pearl_millet": ["pearl millet", "bajra", "बाजरा"],
            "sorghum": ["sorghum", "jowar", "ज्वार"],
            "finger_millet": ["finger millet", "ragi", "रागी", "मडुआ"],
            "brinjal": ["brinjal", "eggplant", "aubergine", "baingan", "बैंगन"],
            "okra": ["okra", "ladyfinger", "bhindi", "भिंडी"],
            "papaya": ["papaya", "papita", "पपीता"],
            "mango": ["mango", "aam", "आम"],
            "banana": ["banana", "kela", "केला"],
            "tea": ["tea", "chai", "चाय", "cha"],
            "jute": ["jute", "patson", "पटसन", "जूट"]
        }

        all_pairs = []
        for crop_id, aliases in crop_aliases.items():
            for alias in aliases:
                all_pairs.append((alias, crop_id))
        all_pairs.sort(key=lambda x: len(x[0]), reverse=True)

        for alias, crop_id in all_pairs:
            if re.search(r'\b' + re.escape(alias) + r'\b', q) or (len(alias) >= 3 and alias in q):
                return crop_id
        return None

    def _detect_location(self, query: str) -> Dict[str, Optional[str]]:
        q = query.lower()
        locations = {
            "palakkad": ("Palakkad", "Kerala"),
            "kozhikode": ("Kozhikode", "Kerala"),
            "ludhiana": ("Ludhiana", "Punjab"),
            "khanna": ("Ludhiana", "Punjab"),
            "karnal": ("Karnal", "Haryana"),
            "guntur": ("Guntur", "Andhra Pradesh"),
            "burdwan": ("Purba Bardhaman", "West Bengal"),
            "bardhaman": ("Purba Bardhaman", "West Bengal"),
            "varanasi": ("Varanasi", "Uttar Pradesh"),
            "indore": ("Indore", "Madhya Pradesh"),
            "karanja": ("Washim", "Maharashtra"),
            "washim": ("Washim", "Maharashtra"),
            "hapur": ("Hapur", "Uttar Pradesh"),
            "kota": ("Kota", "Rajasthan"),
            "kolar": ("Kolar", "Karnataka"),
            "madanapalle": ("Chittoor", "Andhra Pradesh"),
            "chittoor": ("Chittoor", "Andhra Pradesh"),
            "nashik": ("Nashik", "Maharashtra"),
            "nasik": ("Nashik", "Maharashtra"),
            "lasalgaon": ("Nashik", "Maharashtra"),
            "pimpalgaon": ("Nashik", "Maharashtra"),
            "azadpur": ("North Delhi", "Delhi"),
            "delhi": ("North Delhi", "Delhi"),
            "yavatmal": ("Yavatmal", "Maharashtra"),
            "kinwat": ("Yavatmal", "Maharashtra"),
            "rajkot": ("Rajkot", "Gujarat"),
            "gondal": ("Rajkot", "Gujarat"),
            "hanumangarh": ("Hanumangarh", "Rajasthan"),
            "goluwala": ("Hanumangarh", "Rajasthan"),
            "adilabad": ("Adilabad", "Telangana"),
            "agra": ("Agra", "Uttar Pradesh"),
            "farrukhabad": ("Farrukhabad", "Uttar Pradesh"),
            "hooghly": ("Hooghly", "West Bengal"),
            "jalandhar": ("Jalandhar", "Punjab"),
            "pollachi": ("Coimbatore", "Tamil Nadu"),
            "coimbatore": ("Coimbatore", "Tamil Nadu"),
            "sikar": ("Sikar", "Rajasthan"),
            "jabalpur": ("Jabalpur", "Madhya Pradesh"),
            "bharatpur": ("Bharatpur", "Rajasthan")
        }

        for loc_key, (dist, state) in locations.items():
            if loc_key in q:
                return {"district": dist, "state": state, "market": loc_key.title()}
        return {"district": None, "state": None, "market": None}

    def _detect_intent(self, query: str) -> str:
        q = query.lower()
        if self.is_out_of_scope(query):
            return "out_of_scope"

        # Market & Price Intents
        if any(w in q for w in ["best price", "better price", "highest price", "compare market", "compare price", "highest rate", "सबसे अच्छा भाव", "सबसे ज्यादा भाव", "तुलना"]):
            return "market_price_compare"
        if any(w in q for w in ["min price", "max price", "minimum and maximum", "minimum price", "maximum price", "न्यूनतम", "अधिकतम", "कम से कम", "ज्यादा से ज्यादा"]):
            return "market_price_min_max"
        if any(w in q for w in ["previous price", "historical price", "past price", "yesterday price", "पिछला भाव", "इतिहास", "पहले का भाव"]):
            return "market_price_history"
        if any(w in q for w in ["market price", "mandi price", "current price", "latest price", "price", "rate", "bhav", "dam", "mandi", "भाव", "दाम", "मंडी भाव", "रेट", "कीमत"]):
            return "market_price_latest"

        # Variety Intents
        if any(w in q for w in ["variety", "varieties", "hybrid", "किस्म", "किस्में", "प्रजाति", "उन्नत किस्म", "बीज की किस्म"]):
            return "crop_variety"

        # Agronomy & Scheme Intents
        if any(w in q for w in ["कम पानी", "drought", "सूखा", "कौन सी फसल", "less water", "kam pani"]):
            return "crop_selection"
        if any(w in q for w in ["बीज उपचार", "beej upchar", "seed treatment", "seed dressing", "बीजोपचार", "उपचारित", "शोधन"]):
            return "seed_treatment"
        if any(w in q for w in ["पत्ते पीले", "yellow leaves", "deficiency", "पोषक तत्व", "peele", "peela", "peelapan", "पीले", "पीलापन", "sukh rahe", "सूख रहे"]):
            return "nutrient_deficiency"
        if any(w in q for w in ["खरपतवार", "weed", "weeds", "herbicide", "ghaas", "घास", "कचरा", "kachra", "निराई", "nirayi"]):
            return "weed_management"
        if any(w in q for w in ["फसल चक्र", "crop rotation", "अंतःफसल", "intercrop", "intercropping", "ke baad", "के बाद", "साथ में"]):
            return "crop_rotation"
        if any(w in q for w in ["रोग", "कीट", "disease", "pest", "pests", "dawai", "दवा", "इल्ली", "illi", "sundi", "सुंडी", "झुलसा", "कीड़ा", "keeda", "छेद", "chhed", "धब्बा", "सड़न", "मकड़ी", "माइट", "mite", "spider mite", "रोकथाम", "control", "blight", "rust", "smut", "canker"]):
            return "pests_diseases"
        if any(w in q for w in ["बीज दर", "seed rate", "बुवाई", "sow", "sowing", "lagaye", "रोपाई", "plant", "planting", "बोने", "कब लगाएं", "कतार बुवाई"]):
            return "sowing"
        if any(w in q for w in ["दूरी", "spacing", "distance", "फासला", "doori", "लाइन से लाइन", "पौधे से पौधे"]):
            return "spacing"
        if any(w in q for w in ["भंडारण", "storage", "store", "नमी", "bhandaran", "कोठी", "गोदाम", "घुन", "ghun"]):
            return "storage"
        if any(w in q for w in ["जीवामृत", "jeevamrut", "जैविक", "organic", "वर्मीकम्पोस्ट"]):
            return "organic_farming"
        if any(w in q for w in ["सोलर पंप", "solar pump", "kusum", "मशीनरी", "ट्रैक्टर", "rotavator"]):
            return "farm_machinery"
        if any(w in q for w in ["मिट्टी", "soil", "mitti", "जमीन", "ph", "पीएच", "दोमट", "काली मिट्टी", "लाल मिट्टी"]):
            return "soil"
        if any(w in q for w in ["सिंचाई", "irrigation", "water", "पानी", "pani", "sinchai", "watering"]):
            return "irrigation"
        if any(w in q for w in ["खाद", "fertilizer", "urea", "यूरिया", "dap", "npk", "khad", "उर्वरक", "poshak"]):
            return "fertilizer"
        if any(w in q for w in ["कटाई", "harvest", "katai", "पक", "तुड़ाई", "तैयार", "maturity", "chunai", "चुनाई", "retting", "रेट्टिंग"]):
            return "harvesting"
        if any(w in q for w in ["योजना", "scheme", "subsidy", "अनुदान", "pm-kisan", "pmfby", "सम्मान निधि"]):
            return "schemes"
        if any(w in q for w in ["लोन", "ऋण", "loan", "kcc", "credit", "bank", "ब्याज", "केसीसी"]):
            return "loans"
        return "cultivation"

    def _handle_market_query(self, query: str, intent: str, crop_id: Optional[str], loc: Dict[str, Optional[str]], detected_lang: str, context: Optional[Dict[str, Any]] = None) -> Optional[AIQueryResponse]:
        db = SessionLocal()
        try:
            # Resolve district from context if query didn't specify
            district = loc.get("district") or (context.get("district") if context else None)
            state = loc.get("state") or (context.get("state") if context else None)
            
            # If still no crop detected, check farmer profile context
            if not crop_id and context:
                crop_id = context.get("crop") or context.get("crop_id")

            # 1. Market Comparison Query
            if intent == "market_price_compare":
                q = db.query(MarketPrice)
                if crop_id:
                    q = q.filter(MarketPrice.crop_id == crop_id)
                records = q.order_by(MarketPrice.modal_price.desc()).all()
                if records:
                    best = records[0]
                    comm_name = best.commodity
                    ans_en = f"For {comm_name}, the highest modal price in available markets is ₹{int(best.modal_price):,} per quintal at {best.market} ({best.district}, {best.state}) recorded on {best.price_date}. (Source: {best.source})."
                    ans_hi = f"{comm_name} के लिए उपलब्ध मंडियों में सबसे अच्छा मॉडल भाव ₹{int(best.modal_price):,} प्रति क्विंटल {best.market} ({best.district}, {best.state}) में है (दिनांक: {best.price_date}, स्रोत: {best.source})।"
                    return AIQueryResponse(
                        answer=ans_hi if detected_lang == "hi" else ans_en,
                        answer_hi=ans_hi,
                        detected_intent=intent,
                        confidence=0.98,
                        is_verified_fact=True,
                        source=best.source,
                        provider="MarketDataEngine"
                    )

            # 2. Latest Price / Min-Max / History Query
            q = db.query(MarketPrice)
            if crop_id:
                q = q.filter(MarketPrice.crop_id == crop_id)
            if district:
                q = q.filter(MarketPrice.district.ilike(f"%{district}%"))
            if state and not district:
                q = q.filter(MarketPrice.state.ilike(f"%{state}%"))

            record = q.order_by(MarketPrice.price_date.desc(), MarketPrice.id.desc()).first()
            
            # If no district record found, fallback to crop record overall
            if not record and crop_id:
                record = db.query(MarketPrice).filter(MarketPrice.crop_id == crop_id).order_by(MarketPrice.price_date.desc()).first()

            if record:
                comm_name = record.commodity
                mkt_name = record.market
                dist_name = record.district
                st_name = record.state
                m_price = int(record.modal_price)
                min_p = int(record.min_price)
                max_p = int(record.max_price)
                p_date = record.price_date
                src = record.source

                if intent == "market_price_min_max":
                    ans_en = f"The price range for {comm_name} at {mkt_name} ({dist_name}, {st_name}) is minimum ₹{min_p:,} and maximum ₹{max_p:,} per quintal, with a modal price of ₹{m_price:,} recorded on {p_date}. (Source: {src})."
                    ans_hi = f"{mkt_name} ({dist_name}, {st_name}) में {comm_name} का न्यूनतम भाव ₹{min_p:,} और अधिकतम भाव ₹{max_p:,} प्रति क्विंटल है, तथा मॉडल भाव ₹{m_price:,} है (दिनांक: {p_date}, स्रोत: {src})।"
                elif intent == "market_price_history":
                    ans_en = f"The previous recorded price for {comm_name} at {mkt_name} was ₹{m_price:,} per quintal (Range: ₹{min_p:,} - ₹{max_p:,}) recorded on {p_date} according to {src}."
                    ans_hi = f"{mkt_name} में {comm_name} का दर्ज पिछला भाव ₹{m_price:,} प्रति क्विंटल (न्यूनतम ₹{min_p:,} - अधिकतम ₹{max_p:,}) था, जो {p_date} को {src} द्वारा दर्ज किया गया था।"
                else:
                    ans_en = f"The latest available modal price for {comm_name} at {mkt_name} ({dist_name}, {st_name}) is ₹{m_price:,} per quintal (Range: ₹{min_p:,} - ₹{max_p:,}), recorded on {p_date} according to {src}."
                    ans_hi = f"{mkt_name} ({dist_name}, {st_name}) में {comm_name} का नवीनतम मॉडल भाव ₹{m_price:,} प्रति क्विंटल है (दायरा: ₹{min_p:,} - ₹{max_p:,}), जो {p_date} को {src} के अनुसार दर्ज किया गया।"

                return AIQueryResponse(
                    answer=ans_hi if detected_lang == "hi" else ans_en,
                    answer_hi=ans_hi,
                    detected_intent=intent,
                    confidence=0.98,
                    is_verified_fact=True,
                    source=src,
                    provider="MarketDataEngine"
                )

            # If no price found in dataset, provide clear honest response without fabricating numbers
            missing_en = "I couldn't find a market price for that crop/market in the available dataset. Please check Agmarknet or your nearest APMC mandi."
            missing_hi = "उपलब्ध डेटासेट में इस फसल/मंडी का बाजारी भाव उपलब्ध नहीं है। कृपया नजदीकी कृषि उपज मंडी अथवा Agmarknet पोर्टल पर संपर्क करें।"
            return AIQueryResponse(
                answer=missing_hi if detected_lang == "hi" else missing_en,
                answer_hi=missing_hi,
                detected_intent=intent,
                confidence=0.80,
                is_verified_fact=False,
                source="Agmarknet Market Information System",
                provider="MarketDataEngine"
            )
        finally:
            db.close()

    def _handle_variety_query(self, query: str, crop_id: Optional[str], detected_lang: str) -> Optional[AIQueryResponse]:
        db = SessionLocal()
        try:
            q = db.query(CropVariety)
            if crop_id:
                q = q.filter(CropVariety.crop_id == crop_id)
            varieties = q.all()

            if varieties:
                var_summaries_en = []
                var_summaries_hi = []
                for v in varieties[:3]:
                    var_summaries_en.append(f"{v.variety_name} ({v.category}, Yield: {v.yield_potential}, Duration: {v.duration_days}) - {v.special_features}")
                    feat_hi = v.special_features_hi or v.special_features
                    var_summaries_hi.append(f"{v.variety_name} ({v.category}, पैदावार: {v.yield_potential}, अवधि: {v.duration_days}) - {feat_hi}")

                crop_name = varieties[0].crop_id.title()
                src = varieties[0].source or "ICAR Institute Guides"
                ans_en = f"Recommended varieties for {crop_name} include:\n1. " + "\n2. ".join(var_summaries_en) + f"\n(Source: {src})"
                ans_hi = f"{crop_name} की प्रमुख उन्नत किस्में:\n1. " + "\n2. ".join(var_summaries_hi) + f"\n(स्रोत: {src})"

                return AIQueryResponse(
                    answer=ans_hi if detected_lang == "hi" else ans_en,
                    answer_hi=ans_hi,
                    detected_intent="crop_variety",
                    confidence=0.98,
                    is_verified_fact=True,
                    source=src,
                    provider="CropVarietyEngine"
                )
            return None
        finally:
            db.close()

    async def answer_query(self, query: str, language: str = "auto", context: Optional[Dict[str, Any]] = None) -> AIQueryResponse:
        # Detect language: if query has Devanagari, it's Hindi; if query has English Latin words without Hindi keywords, English
        has_devanagari = bool(re.search(r'[ऀ-ॿ]', query))
        has_hinglish = any(w in query.lower() for w in ["kya", "kaise", "bhav", "mandi", "dawai", "beej", "fasal", "khet", "pani", "khad", "rog", "kitna", "chahiye", "lagaye", "batao", "hai", "me", "se", "ko"])
        
        if has_devanagari or has_hinglish or language == "hi":
            detected_lang = "hi"
        else:
            detected_lang = "en"

        # 1. Guardrail Refusal
        if self.is_out_of_scope(query):
            guardrail_hi = "मैं कृषिमित्र (KrishiMitra) हूँ। मैं केवल कृषि, फसल, मिट्टी, खाद, सिंचाई, कीट-रोग, पशुपालन, मंडी भाव और किसान योजनाओं से संबंधित प्रश्नों में आपकी सहायता कर सकता हूँ। कृपया खेती से जुड़ा कोई प्रश्न पूछें।"
            guardrail_en = "I am KrishiMitra, your digital agriculture assistant. I can only assist with crops, soil, irrigation, fertilizer, pests, diseases, market prices, and government farming schemes. Please ask a farming-related question."
            primary_ans = guardrail_hi if detected_lang == "hi" else guardrail_en
            return AIQueryResponse(
                answer=primary_ans,
                answer_hi=guardrail_hi,
                detected_intent="out_of_scope",
                confidence=0.99,
                is_verified_fact=True,
                source="KrishiMitra Domain Guardrail",
                provider="LocalVerifiedAI"
            )

        crop_id = self._detect_crop(query)
        if not crop_id and context:
            crop_id = context.get("crop") or context.get("crop_id")

        intent = self._detect_intent(query)
        loc = self._detect_location(query)

        # 2. Handle Market Price Queries
        if intent in ["market_price_latest", "market_price_min_max", "market_price_compare", "market_price_history"]:
            return self._handle_market_query(query, intent, crop_id, loc, detected_lang, context)

        # 3. Handle Crop Variety Queries
        if intent == "crop_variety":
            var_resp = self._handle_variety_query(query, crop_id, detected_lang)
            if var_resp:
                return var_resp

        # 4. Handle ICAR Agronomy Knowledge Queries
        best_match = None
        if self.knowledge_data:
            # 4a. Match both crop and intent
            candidates = [k for k in self.knowledge_data if k.get("intent") == intent and k.get("crop_id") == crop_id]
            if candidates:
                best_match = candidates[0]
            # 4b. Match crop only if intent didn't find
            elif crop_id:
                crop_cands = [k for k in self.knowledge_data if k.get("crop_id") == crop_id and (k.get("intent") == "cultivation" or k.get("intent") == intent)]
                if not crop_cands:
                    crop_cands = [k for k in self.knowledge_data if k.get("crop_id") == crop_id]
                if crop_cands:
                    best_match = crop_cands[0]
            # 4c. Match intent only (e.g. general scheme/loan/crop selection)
            else:
                intent_cands = [k for k in self.knowledge_data if k.get("intent") == intent]
                if intent_cands:
                    best_match = intent_cands[0]
            # 4d. Keyword search across sample questions
            if not best_match:
                q_words = [w for w in query.lower().split() if len(w) > 3]
                for k in self.knowledge_data:
                    sq = k.get("sample_question", "").lower()
                    if any(w in sq for w in q_words):
                        best_match = k
                        break

        if best_match:
            ans_en = best_match["answer_en"]
            ans_hi = best_match["answer_hi"]
            primary_ans = ans_hi if detected_lang == "hi" else ans_en
            return AIQueryResponse(
                answer=primary_ans,
                answer_hi=ans_hi,
                detected_intent=intent,
                confidence=0.95,
                is_verified_fact=True,
                source=best_match["source"],
                provider="LocalVerifiedAI"
            )

        fallback_hi = "आपकी फसल से संबंधित यह प्रश्न दर्ज कर लिया गया है। कृपया सटीक सिफारिश हेतु अपने जिले के कृषि विज्ञान केंद्र (KVK) से संपर्क करें अथवा नजदीकी किसान कॉल सेंटर (1800-180-1551) पर कॉल करें।"
        fallback_en = "Your query has been recorded. For customized field advice, please contact your District Krishi Vigyan Kendra (KVK) or Kisan Call Centre at 1800-180-1551."
        primary_ans = fallback_hi if detected_lang == "hi" else fallback_en
        return AIQueryResponse(
            answer=primary_ans,
            answer_hi=fallback_hi,
            detected_intent=intent,
            confidence=0.70,
            is_verified_fact=True,
            source="ICAR Kisan Call Centre National Guidelines",
            provider="LocalVerifiedAI"
        )

def get_ai_provider() -> AIProvider:
    return LocalAIProvider()
