# KrishiMitra Voice Assistant Test Queries & Dataset Integration Guide

This document specifies the end-to-end voice query test suite for **KrishiMitra 2**, grounding queries in the uploaded **KRISHIMITRA_AUTHORITY_SOURCE_PACK** and **DAFW/KCC/Agmarknet** datasets.

---

## 1. Architecture Flow

```
Farmer Speaks (Audio Input)
  ↓
Android SpeechRecognizer (Recognizes en-IN / hi-IN via ACTION_RECOGNIZE_SPEECH)
  ↓
Query Text ("What is the current market price of rice?")
  ↓
HybridAIRouter / LocalNLPEngine / FastAPI /ai/query
  ↓
Parameter Extraction (Crop: Rice, District: Context/Query, Intent: market_price_latest)
  ↓
Deterministic Structured DB Lookup (market_prices / crop_varieties tables)
  ↓
Text Response Formulation (Exact numerical values, market name, date, source)
  ↓
Text-to-Speech Engine (Android TTS speak() @ 0.92x speed in Hindi/English)
  ↓
Farmer Hears Answer Aloud
```

---

## 2. Test Suite: Five Priority Crops & Key Commodities

### Query 1: Rice Generic Market Price
* **Spoken Query**: `"What is the current market price of rice?"`
* **Extracted Intent**: `market_price_latest`
* **Extracted Parameters**: `crop = "rice"`, `district = None`, `state = None`
* **Farmer Profile Context**: None
* **Backend Database Lookup**: `SELECT * FROM market_prices WHERE crop_id = 'rice' ORDER BY price_date DESC LIMIT 1`
* **Dataset Record**: Palakkad APMC, Kerala | Modal: ₹2,850/Q | Min: ₹2,700/Q | Max: ₹3,050/Q | Date: 2026-08-30
* **Spoken & Displayed Response**:
  > *"The latest available modal price for Rice / Paddy at Palakkad APMC (Palakkad, Kerala) is ₹2,850 per quintal (Range: ₹2,700 - ₹3,050), recorded on 2026-08-30 according to Kerala State Agricultural Marketing Board."*

---

### Query 2: Contextual Farmer Profile Query (Palakkad + Rice)
* **Spoken Query**: `"What is the price of my crop in my district?"`
* **Extracted Intent**: `market_price_latest`
* **Extracted Parameters**: `crop = None` (resolved from context), `district = None` (resolved from context)
* **Farmer Profile Context**: `{"crop": "rice", "district": "Palakkad"}`
* **Backend Database Lookup**: `SELECT * FROM market_prices WHERE crop_id = 'rice' AND district LIKE '%Palakkad%' ORDER BY price_date DESC LIMIT 1`
* **Dataset Record**: Palakkad APMC | Modal: ₹2,850/Q | Date: 2026-08-30
* **Spoken & Displayed Response**:
  > *"The latest available modal price for Rice / Paddy at Palakkad APMC (Palakkad, Kerala) is ₹2,850 per quintal (Range: ₹2,700 - ₹3,050), recorded on 2026-08-30 according to Kerala State Agricultural Marketing Board."*

---

### Query 3: Specific Mandi Wheat Price (KCC Transcript Record)
* **Spoken Query**: `"What is the price of wheat in Karanja mandi?"`
* **Extracted Intent**: `market_price_latest`
* **Extracted Parameters**: `crop = "wheat"`, `market = "Karanja"`, `district = "Washim"`
* **Farmer Profile Context**: None
* **Backend Database Lookup**: `SELECT * FROM market_prices WHERE crop_id = 'wheat' AND market LIKE '%Karanja%'`
* **Dataset Record**: Karanja Mandi, Washim, Maharashtra | Modal: ₹2,805/Q | Date: 2026-08-30
* **Spoken & Displayed Response (Hindi)**:
  > *"Karanja Mandi (Washim, Maharashtra) में Wheat का नवीनतम मॉडल भाव ₹2,805 प्रति क्विंटल है (दायरा: ₹2,600 - ₹3,050), जो 2026-08-30 को MSAMB Maharashtra / Agmarknet के अनुसार दर्ज किया गया।"*

---

### Query 4: Compare Markets for Cotton
* **Spoken Query**: `"Which market has a better price for cotton?"`
* **Extracted Intent**: `market_price_compare`
* **Extracted Parameters**: `crop = "cotton"`
* **Farmer Profile Context**: None
* **Backend Database Lookup**: `SELECT * FROM market_prices WHERE crop_id = 'cotton' ORDER BY modal_price DESC LIMIT 1`
* **Dataset Record**: Goluwala Mandi (Hanumangarh, Rajasthan) | Modal: ₹8,490/Q | Date: 2026-08-29
* **Spoken & Displayed Response**:
  > *"For Cotton (Kapas), the highest modal price in available markets is ₹8,490 per quintal at Goluwala Mandi (Hanumangarh, Rajasthan) recorded on 2026-08-29. (Source: Rajasthan State Agricultural Marketing Board)."*

---

### Query 5: Tomato Min/Max Price in Kolar
* **Spoken Query**: `"What is the minimum and maximum price of tomato in Kolar?"`
* **Extracted Intent**: `market_price_min_max`
* **Extracted Parameters**: `crop = "tomato"`, `district = "Kolar"`
* **Farmer Profile Context**: None
* **Backend Database Lookup**: `SELECT * FROM market_prices WHERE crop_id = 'tomato' AND district LIKE '%Kolar%'`
* **Dataset Record**: Kolar APMC Mandi, Karnataka | Min: ₹1,400/Q | Max: ₹2,400/Q | Modal: ₹1,950/Q
* **Spoken & Displayed Response**:
  > *"The price range for Tomato at Kolar APMC Mandi (Kolar, Karnataka) is minimum ₹1,400 and maximum ₹2,400 per quintal, with a modal price of ₹1,950 recorded on 2026-08-30. (Source: Karnataka State Agricultural Marketing Board / e-NAM)."*

---

### Query 6: Coconut Latest Price
* **Spoken Query**: `"What is the latest price of coconut?"`
* **Extracted Intent**: `market_price_latest`
* **Extracted Parameters**: `crop = "coconut"`
* **Farmer Profile Context**: None
* **Backend Database Lookup**: `SELECT * FROM market_prices WHERE crop_id = 'coconut' ORDER BY price_date DESC`
* **Dataset Record**: Kozhikode Mandi, Kerala | Modal: ₹11,500/Q (Copra FAQ) | Palakkad: ₹3,100/Q (Dehusked)
* **Spoken & Displayed Response**:
  > *"The latest available modal price for Coconut at Kozhikode Mandi (Kozhikode, Kerala) is ₹11,500 per quintal (Range: ₹10,500 - ₹12,200), recorded on 2026-08-30 according to NAFED / CDB Minimum Support Price Benchmark."*

---

### Query 7: Historical / Previous Price of Potato
* **Spoken Query**: `"What was the previous price of potato?"`
* **Extracted Intent**: `market_price_history`
* **Extracted Parameters**: `crop = "potato"`
* **Farmer Profile Context**: None
* **Backend Database Lookup**: `SELECT * FROM market_prices WHERE crop_id = 'potato' ORDER BY price_date DESC`
* **Dataset Record**: Sheoraphuli Mandi, Hooghly, WB | Modal: ₹1,520/Q | Date: 2026-08-30
* **Spoken & Displayed Response**:
  > *"The previous recorded price for Potato at Sheoraphuli Mandi was ₹1,520 per quintal (Range: ₹1,250 - ₹1,750) recorded on 2026-08-30 according to WB Agmarknet."*

---

### Query 8: High-Yielding Wheat Varieties (ICAR-IIWBR RB-36)
* **Spoken Query**: `"What variety information is available for wheat?"`
* **Extracted Intent**: `crop_variety`
* **Extracted Parameters**: `crop = "wheat"`
* **Backend Database Lookup**: `SELECT * FROM crop_varieties WHERE crop_id = 'wheat'`
* **Dataset Records**: HD 2967 (Pusa Borlaug), DBW 187 (Karan Vandana - Biofortified), HI 1544 (Purna)
* **Spoken & Displayed Response**:
  > *"Recommended varieties for Wheat include:\n1. HD 2967 (Pusa Borlaug) (Timely Sown Irrigated, Yield: 5.5-6.5 tonnes/ha, Duration: 140-145 days) - High yielding, good chapati quality\n2. DBW 187 (Karan Vandana) (High Yielding & Biofortified, Yield: 6.0-7.5 tonnes/ha, Duration: 120-125 days) - High resistance to yellow rust and wheat blast; rich in Iron (43.1 ppm)\n3. HI 1544 (Purna) (Central Zone Timely Sown, Yield: 5.0-5.8 tonnes/ha, Duration: 115-120 days) - Bold lustrous amber grains\n(Source: ICAR-IIWBR Karnal Variety Guide RB-36)"*

---

### Query 9: Rice Drought-Tolerant & High-Yielding Varieties (ICAR-NRRI Bulletin 50)
* **Spoken Query**: `"धान की उन्नत किस्में कौन सी हैं?"`
* **Extracted Intent**: `crop_variety`
* **Extracted Parameters**: `crop = "rice"`
* **Backend Database Lookup**: `SELECT * FROM crop_varieties WHERE crop_id = 'rice'`
* **Dataset Records**: Swarna (MTU 7029), Pusa Basmati 1121, Sahbhagi Dhan (Drought Tolerant), CO 51
* **Spoken & Displayed Response (Hindi)**:
  > *"Rice की प्रमुख उन्नत किस्में:\n1. Swarna (MTU 7029) (Late High Yielding, पैदावार: 5.5-6.5 tonnes/ha, अवधि: 145-150 days) - अधिक कल्ले फूटने वाली, अर्ध-बौनी, उच्च मिलिंग रिकवरी\n2. Pusa Basmati 1121 (Premium Basmati, पैदावार: 4.5-5.0 tonnes/ha, अवधि: 140-145 days) - अत्यधिक लंबा पतला दाना, उत्तम सुगंध\n3. Sahbhagi Dhan (Drought Tolerant, पैदावार: 4.0-4.5 tonnes/ha, अवधि: 115-120 days) - सूखा रोधी, कम पानी में भी अच्छा उत्पादन देने वाली\n(स्रोत: ICAR-NRRI Cuttack & ICAR-IIRR Hyderabad)"*

---

### Query 10: Non-Agricultural Out-of-Scope Guardrail
* **Spoken Query**: `"Who is the prime minister of India?"`
* **Extracted Intent**: `out_of_scope`
* **Extracted Parameters**: None
* **Spoken & Displayed Response**:
  > *"I am KrishiMitra, your digital agriculture assistant. I can only assist with crops, soil, irrigation, fertilizer, pests, diseases, market prices, and government farming schemes. Please ask a farming-related question."*

---

## 3. Offline vs. Online Capabilities

| Capability | Offline (On-Device APK) | Online (FastAPI Backend) |
|---|---|---|
| **Speech Recognition (STT)** | Uses on-device Google Speech Services / Android SpeechRecognizer (supports offline voice model when downloaded on phone) | Browser / Client-side STT |
| **Market Price Lookup** | Pre-seeded SQLite database `krishi_knowledge.db` (60 market records across all major crops) | Live / synced `market_prices` table in PostgreSQL / SQLite |
| **Crop Varieties** | Pre-seeded SQLite `crop_varieties` (ICAR Institute Bulletins) | REST API `/api/v1/market-prices/varieties` |
| **Agronomy Knowledge** | 8,619 facts in SQLite + 664 in `mobile_knowledge_index.json` | 8,619 facts in backend database |
| **Text-to-Speech (TTS)** | Native Android TextToSpeech with Hindi / English offline voices | Client-side TTS synthesis |
