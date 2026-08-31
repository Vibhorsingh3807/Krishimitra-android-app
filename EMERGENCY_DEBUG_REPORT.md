# 🚨 EMERGENCY DEBUG REPORT — AI Agent / LLM Stabilization

**Date/Time**: 2026-09-01
**Project**: KrishiMitra Android App (SIH AI Agriculture Assistant)
**Objective**: Fix AI Agent / LLM crash, stabilize application, verify regression, and build final working DEBUG APK.

---

## 1. Project System Configuration

| Component | Specification / Version |
| :--- | :--- |
| **Android Framework** | Jetpack Compose (BOM 2024.12.01) |
| **Kotlin Version** | 2.0.21 |
| **Android Gradle Plugin (AGP)** | 8.7.3 |
| **Compile SDK / Target SDK** | 35 |
| **Min SDK** | 24 (Android 7.0+) |
| **ONNX Runtime Android** | `com.microsoft.onnxruntime:onnxruntime-android:1.18.0` |
| **CameraX Version** | `1.4.1` |
| **Networking & Serialization** | OkHttp `4.12.0`, Gson `2.11.0` |
| **JVM Target** | Java 17 |

---

## 2. Model & Dataset Packaging Status

| Asset File | Path | Size | Status |
| :--- | :--- | :--- | :--- |
| **Quantized LLM** | `assets/krishi_mini_llm_quantized.onnx` | 1.67 MB | Verified in APK |
| **LLM Vocab** | `assets/vocab.json` | 130 KB | Verified in APK |
| **Quantized Disease Model**| `assets/crop_disease_model_quantized.onnx` | 39.8 KB | Verified in APK |
| **Disease Model Fallback** | `assets/crop_disease_model.onnx` | 67.9 KB | Verified in APK |
| **Disease Labels** | `assets/disease_labels.txt` | 224 B | Verified in APK |
| **Mobile NLP Model** | `assets/mobile_nlp_intent_model.json` | 1.23 MB | Verified in APK |
| **Mobile Knowledge Index** | `assets/mobile_knowledge_index.json` | 748 KB | Verified in APK |
| **SQLite Database** | `assets/krishi_knowledge.db` | 10.08 MB | Verified (497 RAG records, 49 crops, 60 prices) |

---

## 3. Crash Investigation & Root Causes

### 🔴 Primary Crash: `ChatScreen.kt` Configuration Locale Access
- **Location**: `ChatScreen.kt` initial composition line.
- **Trace/Cause**: `val appLocale = LocalConfiguration.current.locales[0].language`. When `LanguageManager` dynamically wrapped the Activity configuration context, `Configuration.locales` on certain Android versions returned an empty `LocaleList`. Accessing `locales[0]` threw an unhandled `java.lang.IndexOutOfBoundsException` during screen composition, causing the app to unexpectedly quit/close **immediately upon tapping AI Agent**.
- **Fix**: Replaced with safe `Locale.getDefault().language` guarded by a try-catch fallback.

### 🟡 Secondary Risk: Uncaught VoiceManager Exceptions
- **Location**: `VoiceManager.kt` -> `startListening()`.
- **Trace/Cause**: Unhandled `throw e` inside `startListening()` when `SpeechRecognizer` service failed to initialize or encountered a hardware/permission error.
- **Fix**: Removed `throw e`, wrapped all speech recognizer and TTS calls in safe try-catch blocks with self-healing re-initialization and graceful in-chat error notices.

### 🟢 Tertiary Risk: Camera Preview Hardware Binding
- **Location**: `CameraScreen.kt`.
- **Trace/Cause**: CameraX `PreviewView` bound to `NavBackStackEntry` without checking `hasCamera(selector)` threw uncaught `IllegalArgumentException` on restricted camera devices/emulators.
- **Fix**: Converted camera scan trigger to safe system camera intent (`ActivityResultContracts.TakePicturePreview()`) and gallery picker, keeping 3 instant demo presets (*स्वस्थ पत्ती / झुलसा रोग / पीला रतुआ*).

---

## 4. Initialization Order & Memory Management Audit

- **App Startup (`KrishiMitraApp.onCreate`)**:
  - SQLite database initialized via `DatabaseHelper.getInstance(this)` (10.08 MB database stored in app internal storage).
  - ONNX Quantized Mini-LLM (1.67 MB) loaded with 2 worker threads. Native memory footprint is ~12 MB, well within standard Android 512 MB Java/Native heap limits.
  - BM25 RAG index built asynchronously in memory (~497 records, < 2 MB RAM).
- **Graceful Failure**:
  - If ONNX session fails or native library cannot load on an unusual ABI, `OnnxLanguageModel` falls back to verified RAG context synthesis.
  - If RAG retrieval returns no facts, it falls back to Local NLP Intent engine or Cloud AI fallback when online.

---

## 5. Subsystem Health Matrix

| Subsystem | Functional Status | Fail-Safe Guarded? |
| :--- | :--- | :--- |
| **AI Agent UI (`ChatScreen`)** | 🟢 STABLE | Yes (locale crash fixed, safe composable state) |
| **Local RAG Engine (`LocalRAGEngine`)** | 🟢 STABLE | Yes (BM25 indexed across 497 facts) |
| **On-device ONNX LLM (`OnnxLanguageModel`)** | 🟢 STABLE | Yes (Quantized 1.67MB model loaded lazily) |
| **NLP Intent Classifier (`LocalNLPEngine`)** | 🟢 STABLE | Yes (Covers mandi prices, crops, schemes) |
| **Remote AI Fallback (`RemoteAIProvider`)** | 🟢 STABLE | Yes (Only attempted when online and low confidence) |
| **Voice Speech-to-Text (`VoiceManager`)** | 🟢 STABLE | Yes (Protected against uncaught throws) |
| **Camera & Disease Scanning (`CameraScreen`)** | 🟢 STABLE | Yes (Native camera intent + gallery + 3 demo presets) |
| **Crop Guide (49 Crops)** | 🟢 STABLE | Yes (Full ICAR manuals) |
| **Mandi Market Prices** | 🟢 STABLE | Yes (60 APMC records + live DB search) |
| **Government Schemes & Loans** | 🟢 STABLE | Yes (8 schemes + 5 loan products) |
