# KrishiMitra 2 (कृषिमित्र) — Sanitized Architectural & Technical Audit

> **Notice**: This report has been sanitized to remove secrets, credentials, tokens, and private user data and is intended for external architecture analysis.

---

## 1. Executive Summary & Philosophy

**KrishiMitra (कृषिमित्र)** is a production-grade, offline-first smart agriculture assistant designed for Indian farmers. It operates primarily on-device without internet connectivity, bridging the advisory gap for rural smallholders using budget Android devices.

* **Primary Philosophy**: 100% on-device functionality for core advisory, computer vision disease scanning, speech interaction, and government scheme exploration.
* **Secondary Tier**: Cloud synchronization and optional LLM fallback when network connectivity is available.
* **Accuracy Policy**: Strictly grounded in verified ICAR (Indian Council of Agricultural Research) and Ministry of Agriculture guidelines with zero hallucinations.

---

## 2. Application Architecture Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    KRISHIMITRA 2 ARCHITECTURE OVERVIEW                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  [ MOBILE CLIENT (Android 7.0+ / Jetpack Compose) ] ── (100% Offline)      │
│  ├── UI Layer: 8 Compose Screens (Home, Chat, Scanner, Crops, Weather, etc.)│
│  ├── Voice Pipeline: Android SpeechRecognizer + TextToSpeech (hi-IN/en-IN)  │
│  ├── Disease Vision: ONNX Runtime Mobile (MobileAgriNet INT8, 38.9 KB)      │
│  ├── NLP Engine: Pure Kotlin TF-IDF Intent Classifier (10 intents)          │
│  └── Storage: Pre-seeded Room SQLite DB (krishi_knowledge.db, 1.16 MB)      │
│                                │                                            │
│                                │ (Optional Cloud Fallback when Online)      │
│                                ▼                                            │
│  [ BACKEND SERVICE (Python 3.13 / FastAPI / Uvicorn) ]                      │
│  ├── REST Endpoints: /crops, /diseases, /schemes, /loans, /weather, /ai/query│
│  ├── Database: Local SQLite (krishimitra.db, SQLAlchemy ORM)                │
│  ├── Weather Service: Open-Meteo API integration with agro-advisories       │
│  └── AI Provider: Local rule/TF-IDF engine with optional Gemini/OpenAI fallback│
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Component Breakdown

| Layer | Technology | Details |
| :--- | :--- | :--- |
| **Frontend / Mobile Client** | Android Native (Kotlin 2.0, Jetpack Compose, AGP 8.7+) | Complete 8-screen UI localized in Hindi (`values-hi`) and English (`values`). Pre-compiled APK binary available (`app-debug.apk`). |
| **Backend API Service** | Python 3.13 + FastAPI + Uvicorn | High-performance asynchronous REST API server. |
| **Mobile Embedded Database** | SQLite / Room (`krishi_knowledge.db`, 1.16 MB) | Pre-packaged inside Android assets; copied to app storage on first run. |
| **Backend Database** | SQLite (`krishimitra.db`) | Local database managed via SQLAlchemy 2.0 ORM. |
| **Authentication & Users** | **None** | Frictionless, zero-login design for rural farmers. |
| **Computer Vision Engine** | ONNX Runtime Mobile | MobileAgriNet INT8 quantized CNN (`crop_disease_model_quantized.onnx`, 38.9 KB) classifying leaf images into 12 categories. |
| **On-Device NLP Engine** | Pure Kotlin TF-IDF Classifier | Unigram/bigram tokenization + IDF weighting + linear class coefficients. Latency < 3 ms on CPU. |
| **Voice Input (STT)** | Android `SpeechRecognizer` | Native platform speech-to-text supporting `hi-IN` and `en-IN`. |
| **Voice Output (TTS)** | Android `TextToSpeech` | Native platform text-to-speech configured at 0.92x playback speed for rural comprehension. |
| **RAG / Vector Database** | **None** | No vector databases (Chroma, Pinecone, FAISS), no neural embeddings, and no vector RAG exist in the current architecture. |
| **External Integrations** | Open-Meteo Weather API | Geocoded meteorological forecasts with automated agro-advisory rule generation. |
| **Cloud AI Fallbacks** | OpenAI / Google Gemini | Optional fallback in backend when local NLP confidence is low. Configurable via environment variables (keys: `[REDACTED]`). |

---

## 3. Repository Directory Structure

```
.
├── android/                         # Complete Jetpack Compose Android Application
│   ├── app/src/main/
│   │   ├── java/com/krishimitra/app/
│   │   │   ├── data/
│   │   │   │   ├── local/           # DatabaseHelper.kt (SQLite / Room helper)
│   │   │   │   └── remote/          # ApiClient.kt (Retrofit / OkHttp backend client)
│   │   │   ├── domain/
│   │   │   │   ├── ai/              # HybridAIRouter.kt (Local vs Cloud router)
│   │   │   │   └── model/           # Models.kt (Crop, Disease, Scheme, Loan, Chat)
│   │   │   ├── ml/                  # LocalNLPEngine.kt, OnnxDiseaseClassifier.kt
│   │   │   ├── ui/
│   │   │   │   ├── components/      # UI widgets, cards, banners
│   │   │   │   ├── navigation/      # NavGraph.kt, Screen.kt
│   │   │   │   ├── screens/         # Home, Chat, Camera, CropGuide, Weather, Schemes, Loans, Sources
│   │   │   │   └── theme/           # Color.kt, Theme.kt, Type.kt
│   │   │   └── voice/               # VoiceManager.kt (SpeechRecognizer & TTS)
│   │   ├── assets/                  # ONNX models, vocab, pre-seeded krishi_knowledge.db
│   │   └── res/                     # Strings, layouts, drawables (Hindi & English)
│   └── build.gradle.kts
├── backend/                        # Python FastAPI Backend Service
│   ├── app/
│   │   ├── api/v1/                  # Endpoints: ai, crops, diseases, health, loans, schemes, sync, weather
│   │   ├── core/                    # config.py, logging.py
│   │   ├── db/                      # models.py, seed_data.py, session.py
│   │   ├── schemas/                 # Pydantic request/response schemas
│   │   └── services/                # ai_provider.py, weather_service.py
│   ├── tests/                       # test_api.py (Automated Pytest suite)
│   └── requirements.txt
├── data/                            # Verified ICAR & Ministry Knowledge Datasets
│   ├── verified_crops.json          # 15 Indian crops agronomic data
│   ├── verified_diseases.json       # 12 disease classes with organic/chemical remedies
│   ├── verified_schemes.json        # 8 Central/State agricultural schemes
│   ├── verified_loans.json          # 5 Institutional farm credit products
│   └── generated_training_questions.json # 1,424 synthetic QA training pairs
├── ml_pipeline/                     # Reproducible ML & Quantization Pipeline
│   ├── prepare_dataset.py           # Dataset generator across Hindi, English, Hinglish
│   ├── train_nlp.py                 # TF-IDF intent model trainer & JSON exporter
│   ├── train_disease.py             # MobileAgriNet CNN trainer (PyTorch)
│   ├── export_quantize.py           # INT8 / UINT8 ONNX quantizer & benchmark
│   └── generate_android_assets.py   # Builds krishi_knowledge.db & copies assets
├── app-debug.apk                    # Compiled, standalone Android application binary
└── krishimitra.db                   # Local backend SQLite database
```

---

## 4. Voice Assistant & NLP Implementation

### Complete Voice & Text Execution Flow

```
1. Farmer Speaks (Hold-to-Talk touch gesture)
   │
   ▼
2. Speech-to-Text (VoiceManager.kt -> Android Native SpeechRecognizer)
   │
   ▼ (Recognized Query String)
3. ChatScreen (sendMessage() appends query to in-memory state)
   │
   ▼
4. HybridAIRouter (routeQuery() evaluates confidence & connectivity)
   ├── Primary Path: LocalNLPEngine.answerQuery() [Confidence >= 0.70 or Offline]
   └── Fallback Path: CloudAIProvider.answer() [Low Confidence and Online]
   │
   ▼
5. LocalNLPEngine Execution:
   ├── detectCrop(query): Checks aliases for 15 supported crops
   ├── predictIntent(query): Evaluates 10 intent classes via TF-IDF dot product
   └── answerQuery(): Searches mobile_knowledge_index.json for (cropId, intent)
   │
   ▼
6. Response Rendering: Bilingual ICAR-attributed text bubble displayed
   │
   ▼
7. Text-to-Speech (VoiceManager.kt -> Android TextToSpeech speaks aloud at 0.92x speed)
```

### Why the Voice Assistant Currently Has Limited Question Coverage
1. **Static Index Size**: `LocalNLPEngine.kt` queries `mobile_knowledge_index.json`, which contains **128 discrete pre-indexed Q&A records**.
2. **Crop Whitelist**: `detectCrop()` only recognizes **15 hardcoded crop aliases**.
3. **Intent Whitelist**: `predictIntent()` only recognizes **10 fixed intents** (`soil`, `irrigation`, `fertilizer`, `sowing`, `harvesting`, `schemes`, `loans`, `pests_diseases`, `cultivation`, `disease`).
4. **Lookup Rule**: The engine attempts an exact key match:
   ```kotlin
   var match = knowledgeFacts.firstOrNull { it.intent == intent && it.cropId == detectedCrop }
   ```
5. Any question regarding crops outside the 15 or topics outside the 10 intents falls back to the generic Kisan Call Centre helpline notice.

---

## 5. Existing Agricultural Knowledge & Crop Coverage

### Database Schema & Tables

Both the mobile SQLite database (`krishi_knowledge.db`) and backend SQLite database (`krishimitra.db`) share the following relational structure:

* **`crops` (15 records)**:
  * Fields: `id`, `name_en`, `name_hi`, `scientific_name`, `category`, `category_hi`, `soil`, `soil_hi`, `soil_ph`, `climate`, `climate_hi`, `temperature`, `sowing_season`, `sowing_season_hi`, `irrigation`, `irrigation_hi`, `fertilizer`, `fertilizer_hi`, `harvesting`, `harvesting_hi`, `pests`, `pests_hi`, `diseases`, `diseases_hi`, `cultivation_tips`, `cultivation_tips_hi`, `source`, `source_url`.
* **`diseases` (12 records)**:
  * Fields: `id`, `crop`, `crop_hi`, `disease_name_en`, `disease_name_hi`, `pathogen`, `symptoms_en`, `symptoms_hi`, `causes_en`, `causes_hi`, `treatment_organic_en`, `treatment_organic_hi`, `treatment_chemical_en`, `treatment_chemical_hi`, `prevention_en`, `prevention_hi`, `confidence_threshold`.
* **`schemes` (8 records)**:
  * Fields: `id`, `name_en`, `name_hi`, `category`, `category_hi`, `ministry`, `benefits_en`, `benefits_hi`, `eligibility_en`, `eligibility_hi`, `application_process_en`, `application_process_hi`, `official_url`, `source`, `last_verified`.
* **`loans` (5 records)**:
  * Fields: `id`, `bank_name`, `bank_name_hi`, `loan_type`, `loan_type_hi`, `purpose_en`, `purpose_hi`, `interest_rate`, `interest_rate_hi`, `max_limit`, `max_limit_hi`, `eligibility_en`, `eligibility_hi`, `documents_required`, `documents_required_hi`, `official_url`, `source`, `last_verified`.
* **`knowledge_facts` (1,424 records in mobile DB / 300 in backend DB)**:
  * Fields: `id`, `intent`, `crop_id`, `language`, `question`, `answer_en`, `answer_hi`, `source`.

### Crop Coverage Matrix

| Category | Crops Covered | Status |
| :--- | :--- | :--- |
| **Cereals** | Rice/Paddy (`rice`), Wheat (`wheat`), Maize (`maize`) | Fully Supported (Agronomy + NLP) |
| **Cash Crops** | Cotton (`cotton`), Sugarcane (`sugarcane`) | Fully Supported (Agronomy + NLP) |
| **Oilseeds** | Mustard (`mustard`), Soybean (`soybean`), Groundnut (`groundnut`) | Fully Supported (Agronomy + NLP) |
| **Pulses** | Chickpea/Gram (`chickpea`) | Fully Supported (Agronomy + NLP) |
| **Vegetables & Tubers** | Potato (`potato`), Tomato (`tomato`), Onion (`onion`), Chilli (`chilli`) | Fully Supported (Agronomy + NLP) |
| **Fruits** | Mango (`mango`), Banana (`banana`) | Fully Supported (Agronomy + NLP) |
| **Computer Vision (Leaf Scanner)** | Rice, Wheat, Cotton, Potato, Tomato (9 disease classes + Healthy + Soil + Uncertain) | Fully Supported (ONNX Model) |
| **Missing Millets** | Bajra (Pearl Millet), Jowar (Sorghum), Ragi (Finger Millet) | **Not Implemented** |
| **Missing Pulses** | Arhar/Tur (Pigeon Pea), Moong (Green Gram), Urad (Black Gram) | **Not Implemented** |
| **Missing Spices** | Turmeric, Ginger, Garlic, Cumin, Coriander | **Not Implemented** |
| **Missing Commercial / Plantation** | Tea, Coffee, Jute, Tobacco, Rubber | **Not Implemented** |

---

## 6. Offline vs. Online Capabilities

| Component | Offline Status | Online Requirement | Technical Reason |
| :--- | :---: | :---: | :--- |
| **Compose User Interface** | ✅ 100% Offline | None | All UI components, graphics, fonts, and layouts are compiled into APK. |
| **Crop Knowledge Directory** | ✅ 100% Offline | None | Reads directly from embedded SQLite `krishi_knowledge.db`. |
| **Disease Vision Scanner** | ✅ 100% Offline | None | On-device ONNX runtime processes camera bitmaps locally. |
| **Local AI Q&A Engine** | ✅ 100% Offline | None | Pure Kotlin matrix multiplication over pre-packaged JSON vocab. |
| **Voice Input (STT)** | ✅ 100% Offline* | Optional | Android `SpeechRecognizer` operates offline when language packs are present. |
| **Voice Playback (TTS)** | ✅ 100% Offline | None | Android native `TextToSpeech` engine is on-device. |
| **Government Schemes** | ✅ 100% Offline | None (External links require internet) | Scheme rules, criteria, and benefits are cached locally. |
| **Institutional Loans** | ✅ 100% Offline | None | Loan interest rates, limits, and required documents are stored in DB. |
| **Weather Advisories** | ⚠️ Hybrid | Live Updates | Uses 7-district climatological baseline offline; calls Open-Meteo online. |
| **Cloud AI Fallback** | ❌ Online Only | Required | Requires internet to reach backend FastAPI / Cloud LLMs. |
| **Differential DB Sync** | ❌ Online Only | Required | Connects to backend `/api/v1/sync` to pull newly verified agricultural advisories. |

---

## 7. Data Duplication & Potential Conflict Risks

1. **JSON Datasets vs. SQLite Tables**:
   * `data/verified_crops.json` and `data/verified_diseases.json` duplicate data with tables in `krishi_knowledge.db` and `krishimitra.db`.
   * *Architecture Rule*: When updating JSON datasets, the build script `ml_pipeline/generate_android_assets.py` must be executed to regenerate the SQLite databases and mobile indices.
2. **Intent Models vs. Knowledge Index**:
   * `mobile_nlp_intent_model.json` (model weights) and `mobile_knowledge_index.json` (Q&A lookup) are generated from `generated_training_questions.json`.
   * *Architecture Rule*: When adding new question patterns or crops, retrain via `ml_pipeline/train_nlp.py` and rebuild assets.
3. **Kotlin vs. Python Crop Detectors**:
   * `LocalNLPEngine.kt` (Android) and `ai_provider.py` (Backend) contain duplicate crop alias maps and keyword rules. Both must be updated simultaneously when introducing new crops.

---

## 8. Safe Extension Strategy for New Datasets & RAG

### Recommended New Dataset Categories (Authoritative ICAR / GoI Sources)

1. **ICAR Crop Agronomy Expansion**:
   * Add 15 additional major Indian crops (Millets: Bajra, Jowar, Ragi; Pulses: Arhar, Moong, Urad; Spices: Garlic, Ginger, Turmeric, Cumin; Fruits: Apple, Citrus, Papaya, Guava).
   * Storage: `data/verified_crops.json` ➔ Re-seed SQLite `crops` table.
2. **Comprehensive Integrated Pest Management (IPM)**:
   * Biological controls (Trichoderma viride, Pseudomonas fluorescens, Neem kernel extract), economic threshold levels (ETL), and CIBRC-approved chemical dosages.
   * Storage: `data/verified_diseases.json` ➔ Re-seed SQLite `diseases` table.
3. **State-Specific Schemes & Subsidies**:
   * Subsidies for solar pumps (PM-KUSUM), micro-irrigation (Per Drop More Crop), farm mechanization (SMAM), and state-level disaster relief schemes (UP, MP, Bihar, Rajasthan, Maharashtra, Punjab).
   * Storage: `data/verified_schemes.json` ➔ Re-seed SQLite `schemes` table.
4. **Live Mandi Market Prices (Agmarknet)**:
   * Dynamic daily commodity arrival volumes and minimum/maximum/modal prices from APMC markets across India via data.gov.in.
   * Storage: Dynamic backend endpoint (`/api/v1/mandi`) with on-device caching.

### Architecture Recommendations for RAG & Embeddings

* **On-Device Constraints**: Android budget devices cannot run heavy 500 MB+ embedding models.
* **Recommended Hybrid RAG Pattern**:
  1. **Tier 1 (Offline On-Device)**: Expand SQLite Full-Text Search (FTS5) or BM25 indexing over `knowledge_facts` inside `krishi_knowledge.db`. This provides instant keyword & semantic-like search without neural latency.
  2. **Tier 2 (Cloud / Backend RAG)**: For complex farmer queries when online, implement dense vector embeddings (e.g. BGE-M3 / MiniLM / OpenAI embeddings) in the FastAPI backend or Supabase pgvector, retrieving authoritative ICAR research bulletins.

---

## 9. Sensitive Information Audit Confirmation

* **API Keys & Secrets**: None contained or printed (all references replaced with `[REDACTED]`).
* **Passwords & Hashes**: None present.
* **Personal Data & Farmer Records**: None present.
* **Authentication Tokens & JWTs**: None present.
* **Private Repository Credentials**: None present.

*This report is verified as sanitized and ready for external architectural analysis and planning.*
