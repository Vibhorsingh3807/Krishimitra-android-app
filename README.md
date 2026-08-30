# KrishiMitra (कृषिमित्र) — AI-Powered Smart Agriculture Assistant

> **"आपका डिजिटल कृषि साथी — Your Intelligent Farming Companion"**

KrishiMitra is a production-grade, offline-first Android application and accompanying edge ML/backend system specifically engineered for Indian farmers. It bridges the critical agricultural advisory gap for rural smallholders and marginal farmers operating budget Android smartphones with limited RAM (2–3 GB), weak CPUs, and intermittent or absent internet connectivity.

---

## 🌟 Key Capabilities (Phase 2 Upgrade)

* **🤖 On-Device Small Language Model (`KrishiMiniLM`)**:
  * Genuine causal Transformer language model (1,133,568 parameters) trained specifically for agricultural question answering in Hindi and English.
  * Ultra-compact dynamic UINT8 quantization (**1.60 MB** ONNX model).
  * Ultra-low latency: **5.55 ms** CPU inference on mobile without UI lag.
  * Strictly conditioned on RAG context: zero hallucinations, zero fabricated dosages.
* **📚 Local BM25 RAG Retrieval Engine**:
  * 100% offline retrieval running in pure Kotlin in **1.42 ms**.
  * Pre-seeded knowledge across **25 Indian crops**, 12 plant diseases, 8 central government schemes, and 5 institutional loans.
  * Dynamic crop alias boosting ($1.5\times$) for Devanagari, English, and Hinglish queries.
* **🌾 Farmer Community Experience Memory (`FARMER_EXPERIENCE`)**:
  * Dedicated storage in an isolated SQLite table (`farmer_experiences`).
  * Never retrains model weights or treats observations as verified facts.
  * Displayed with clear cautionary attribution: *"🌾 कुछ किसानों के अनुभव (अपुष्ट रिपोर्ट): • उत्तर प्रदेश के किसान: '...' / Some farmer reports suggest..."*
* **🌐 Global Bilingual Language Switch (`English | हिन्दी`)**:
  * One-tap persistent switch in the TopBar (`[EN | हिन्दी]`) affecting all screens, navigation, dialogs, and TTS immediately via Compose `CompositionLocalProvider`.
* **🎙️ Push-to-Talk Voice Assistant (`AUTO | हिन्दी | English`)**:
  * Three explicit selectable modes:
    * `AUTO`: Detects language script (Devanagari vs Latin) and Hinglish keywords dynamically.
    * `हिन्दी`: Locked to Hindi STT, Hindi RAG, and Hindi TTS.
    * `ENGLISH`: Locked to Indian English STT, English RAG, and English TTS.
  * Graceful offline STT/TTS checks and fallback handling.
* **🔬 Quantized Crop Disease Scanner (`MobileAgriNet`)**:
  * Camera leaf viewfinder with edge-framing assistance.
  * Quantized on-device neural classifier (**38.96 KB INT8**, **2.23 ms latency**) targeting 12 Indian agricultural classes.
  * Provides observed symptoms, organic/IPM remedies, verified ICAR chemical dosages, and prevention rules.
* **🌦️ Actionable Agricultural Weather**:
  * Live localized temperature, humidity, wind, and rain probabilities from Open-Meteo with offline district baseline fallbacks.
* **📦 Compiled Standalone Debug APK Ready to Deploy**:
  * Compiled directly via Gradle: **`app-debug.apk`** (88.85 MB, fully self-contained with embedded ONNX models and SQLite database).

---

## 📱 Repository Structure

```
├── android/                         # Complete Jetpack Compose Android Application
│   ├── app/src/main/
│   │   ├── java/com/krishimitra/app/
│   │   │   ├── domain/language/    # LanguageManager & AppLanguage (English / हिन्दी)
│   │   │   ├── domain/rag/         # LocalRAGEngine (BM25 sparse ranking in pure Kotlin)
│   │   │   ├── ml/                 # OnnxLanguageModel & OnnxDiseaseClassifier
│   │   │   ├── voice/              # VoiceManager & LanguageDetector (AUTO mode)
│   │   │   ├── ui/                 # Material 3 Screens & Components
│   │   │   └── data/               # DatabaseHelper (SQLite 25 crops, RAG & experiences)
│   │   ├── assets/                 # Pre-seeded SQLite DB, KrishiMiniLM INT8, Vision INT8
│   │   └── res/                    # Dual localization trees (values & values-hi)
│   └── build.gradle.kts            # Android Gradle configuration
├── ml/                              # Dedicated Phase 2 ML Pipeline
│   ├── data/                       # RAG QA dataset, farmer experiences, triplets
│   ├── training/                   # train_local_llm.py (KrishiMiniLM PyTorch GPU training)
│   ├── export/                     # export_quantize_llm.py (INT8 ONNX quantization)
│   ├── evaluation/                 # evaluate_model.py (Fixed bilingual test suite)
│   └── scripts/                    # build_full_database.py (SQLite database builder)
├── backend/                        # Production-grade Python FastAPI service
│   ├── app/                        # REST APIs, SQLAlchemy models, AI provider abstraction
│   └── tests/                      # Pytest automated test suite (9/9 passing)
├── data/                           # Verified ICAR, Ministry & Banking Seed Datasets
│   ├── verified_crops.json         # 25 Indian crops with ICAR agronomic parameters
│   ├── verified_diseases.json      # 12 disease classes with symptoms and treatments
│   ├── verified_schemes.json       # Central & State agricultural schemes
│   └── verified_loans.json         # Institutional farm loan terms
├── app-debug.apk                   # Final compiled standalone Android APK (88.85 MB)
├── RAG.md                          # RAG architecture, BM25 formula, & memory isolation
├── VOICE.md                        # Voice assistant architecture & offline speech
├── LOCALIZATION.md                 # Complete bilingual system documentation
├── MODEL_EVALUATION.md             # Benchmark evaluation on fixed test set
├── PERFORMANCE.md                  # Low-end device profiling & latency benchmarks
└── ARCHITECTURE.md                 # End-to-end system design & Mermaid diagrams
```

---

## 🚀 How to Run and Test

### 1. Install & Test the Android App
Transfer `app-debug.apk` to any Android smartphone running Android 8.0+ (API 26+) and install:
```bash
adb install -r app-debug.apk
```
* **Offline AI Test**: Put device in **Airplane Mode**. Ask: *"What soil is best for rice?"* or *"धान में कौन सी खाद डालें?"*. Notice instant grounded answer with verified ICAR badge!
* **Language Switch Test**: Tap `[EN | हिन्दी]` in the TopBar. Watch every screen seamlessly translate without reloading.
* **Farmer Experience Test**: Ask about wheat, rice, or mustard. Observe the separate community observation note.
* **Camera Test**: Point camera at a leaf or use the sample buttons (*स्वस्थ पत्ती*, *झुलसा रोग*, *पीला रतुआ*) to see real-time inference.

### 2. Run ML Pipeline Locally
```bash
# Prepare RAG records and triplets
python ml/data/prepare_rag_dataset.py

# Rebuild full SQLite database and deploy to Android assets
python ml/scripts/build_full_database.py

# Train KrishiMiniLM on GPU
python ml/training/train_local_llm.py

# Quantize to INT8 and benchmark
python ml/export/export_quantize_llm.py

# Run benchmark evaluation
python ml/evaluation/evaluate_model.py
```

### 3. Run Backend Server
```bash
cd backend
python -m uvicorn app.main:app --reload --port 8000
```
