# LOCALIZATION.md — Complete Bilingual Architecture (Hindi & English)

KrishiMitra implements an end-to-end bilingual architecture where Hindi and English are first-class citizens. The application defaults to Hindi (`hi-IN`) to prioritize Indian rural farmers while supporting seamless one-tap switching to English (`en-IN`) anywhere in the application.

---

## 1. Global Language Switch Architecture

```
                  ┌─────────────────────────────────────┐
                  │ TopBar Language Switch [EN | हिन्दी] │
                  └──────────────────┬──────────────────┘
                                     │ User click
                                     ▼
                  ┌─────────────────────────────────────┐
                  │    LanguageManager.toggleLanguage()  │
                  └─────────┬───────────────────────────┘
                            │ Persists in SharedPreferences
                            ▼
                  ┌─────────────────────────────────────┐
                  │  currentLanguage: StateFlow<AppLang> │
                  └─────────┬───────────────────────────┘
                            │ Observed reactively
                            ▼
          ┌─────────────────────────────────────────────────────┐
          │ CompositionLocalProvider(                           │
          │   LocalContext provides localizedContext,           │
          │   LocalConfiguration provides localizedConfig       │
          │ )                                                   │
          └─────────────────────────┬───────────────────────────┘
                                    │
       ┌────────────────────────────┼────────────────────────────┐
       ▼                            ▼                            ▼
┌──────────────┐             ┌──────────────┐             ┌──────────────┐
│  All Strings │             │ SQLite Data  │             │ Speech & TTS │
│ Re-evaluated │             │ Hindi/English│             │ Locale Synced│
└──────────────┘             └──────────────┘             └──────────────┘
```

---

## 2. Dynamic Runtime Locale Injection

Instead of requiring an Activity restart or recreate cycle that drops user state or ongoing camera sessions, KrishiMitra dynamically wraps the Jetpack Compose tree:

```kotlin
// In MainActivity.kt
val currentLang by app.languageManager.currentLanguage.collectAsState()
val localizedContext = remember(currentLang) {
    app.languageManager.applyLocaleToConfiguration(this@MainActivity, currentLang.locale)
}

CompositionLocalProvider(
    LocalContext provides localizedContext,
    LocalConfiguration provides localizedContext.resources.configuration
) {
    KrishiMitraTheme {
        // All screens, dialogs, and navigation update instantaneously
    }
}
```

---

## 3. Bilingual Resource & Screen Coverage Matrix

| Screen | Hindi Content (`values-hi`) | English Content (`values`) | Dynamic Data Binding |
| :--- | :--- | :--- | :--- |
| **TopBar** | "ऑनलाइन" / "ऑफ़लाइन" status pill, "हिन्दी" badge | "Online" / "Offline" status pill, "EN" badge | Re-evaluates instantly on toggle |
| **Navigation** | मुख्य पृष्ठ, सहायक, कैमरा, मौसम, योजनाएं | Home, Assistant, Camera, Weather, Schemes | Full bottom bar localization |
| **Assistant (Chat)** | Greeting, sample chips, verified ICAR tags in Hindi | Greeting, sample chips, verified ICAR tags in English | Conditioned via RAG language matching |
| **Crop Guide** | 25 crops with Hindi names, NPK, sowing, soil, and tips | 25 crops with English names, scientific names, and parameters | Toggle between `soilHi`/`soil` and `nameHi`/`nameEn` |
| **Government Schemes** | Benefits, eligibility, application in Hindi | Benefits, eligibility, application in English | Database dual-column reflection |
| **Institutional Loans** | Bank names, interest, limits, documents in Hindi | Bank names, interest, limits, documents in English | Database dual-column reflection |
| **Disease Diagnosis** | Symptoms, organic remedies, chemical remedies in Hindi | Symptoms, organic remedies, chemical remedies in English | OnnxDiseaseClassifier mapped to dual strings |
| **Weather & Advisory** | Real-time agricultural advisories in Hindi | Real-time agricultural advisories in English | Open-Meteo parsed with dual weather condition strings |
