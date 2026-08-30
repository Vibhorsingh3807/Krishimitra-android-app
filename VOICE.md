# VOICE.md — Voice Assistant Architecture & Offline Voice Specification

The KrishiMitra Voice Assistant is designed to serve semi-literate and rural farmers across India through a reliable, natural, push-to-talk bilingual speech interface that functions both offline and online.

---

## 1. The Push-to-Talk Pipeline

To prevent battery drain, background eavesdropping, and accidental activation, KrishiMitra uses an explicit **Push-to-Talk (Hold-to-Speak)** paradigm:

```
[ PRESS MIC BUTTON ] ──> UI transitions to RECORDING (Red Mic indicator)
         │
[ HOLD & SPEAK ]     ──> Audio captured via SpeechRecognizer (offline preferential)
         │
[ RELEASE BUTTON ]   ──> Recording ends; UI transitions to PROCESSING (Amber indicator)
         │
[ STT TRANSCRIPTION] ──> Raw text emitted (Devanagari, Latin, or Hinglish)
         │
[ LANGUAGE DETECTOR] ──> Script & Keyword inspection determines response language
         │
[ LOCAL RAG + LLM ]  ──> Grounded context retrieved and synthesized (< 10 ms)
         │
[ TTS SYNTHESIS ]    ──> UI transitions to SPEAKING (Green indicator); Audio plays at 0.92x speed
```

---

## 2. The Three Voice Modes

The user can select between three clear modes via the top chip in the Assistant screen:

| Mode | Input Recognition | Language Routing | Output Speech | Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **`AUTO`** | Bilingual `hi-IN` + `en-IN` | Script analysis (Devanagari vs Latin vs Hinglish) | Matched to query language | Default mode for mixed Hindi/English/Hinglish speech |
| **`हिन्दी` (Hindi)** | Locked to `hi-IN` | Forces Hindi RAG & local knowledge | Devanagari Hindi TTS | Monolingual Hindi farmers |
| **`ENGLISH`** | Locked to `en-IN` | Forces English RAG & local knowledge | Indian English TTS | English-preferring users |

---

## 3. Language Script & Dialect Detection (`LanguageDetector.kt`)

In `AUTO` mode, simple "try English first" heuristics fail because Indian farmers frequently mix English and Hindi (Hinglish). KrishiMitra applies a multi-stage detector:

1. **Devanagari Script Check**:
   * Inspects Unicode code point range `0x0900..0x097F`.
   * If $\ge 15\%$ of characters belong to Devanagari script, the query is definitively classified as **`HINDI`**.
2. **Hinglish Phonetic Keyword Matcher**:
   * Scans words written in Latin alphabet against agricultural and question stop-words (`kya`, `kaise`, `pani`, `gehu`, `dhan`, `khad`, `jhulsa`, `sinchai`, `fasal`, `patte`, `upchar`).
   * If matched, routes query as **`HINDI`** to provide the farmer with a Hindi synthesized answer and Hindi TTS.
3. **English Fallback**:
   * Standard English agronomic terminology routes to **`ENGLISH`**.

---

## 4. Offline Hindi Speech-to-Text (STT)

* **Architecture**: Android's `SpeechRecognizer` with `RecognizerIntent.EXTRA_PREFER_OFFLINE = true`.
* **Hardware Requirement**: For 100% offline operation on Android, the device's Google Speech Services must have the offline Hindi (`हिन्दी (भारत)`) speech recognition pack installed (~25 MB).
* **Graceful Degradation**: If the user is offline and the speech pack has not yet been downloaded, KrishiMitra catches `ERROR_NETWORK` and displays a clear notice:
  > *"Offline speech pack may not be downloaded for this language. Please connect to WiFi once to enable offline voice recognition."*

---

## 5. Offline Hindi Text-to-Speech (TTS)

* **Verification**: At application launch, `VoiceManager` calls:
  ```kotlin
  val avail = tts?.isLanguageAvailable(Locale("hi", "IN"))
  isHindiTtsAvailable = (avail != TextToSpeech.LANG_NOT_SUPPORTED && avail != TextToSpeech.LANG_MISSING_DATA)
  ```
* **Fallback Behavior**:
  * If offline Hindi TTS voice data is present, answers are spoken in clear Indian Hindi at a relaxed `0.92x` speech rate.
  * If offline Hindi voice data is absent, the system gracefully falls back to the default device synthesizer without throwing exceptions or crashing.
