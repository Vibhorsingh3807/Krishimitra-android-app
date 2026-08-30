package com.krishimitra.app.voice

import java.util.Locale

enum class DetectedLanguage {
    HINDI,
    ENGLISH,
    HINGLISH
}

object LanguageDetector {

    private val HINGLISH_KEYWORDS = setOf(
        "kya", "kaise", "kab", "kitna", "kitni", "kare", "karein", "karna", "chahiye",
        "pani", "sinchai", "khad", "urea", "dap", "mitti", "gehu", "dhan", "alu", "aaloo",
        "tamatar", "patte", "peele", "yellow", "jhulsa", "rog", "keet", "dawa", "dawai",
        "fasal", "kheti", "daal", "daale", "daalein", "sow", "rokne", "upchar", "bimaar",
        "yojana", "kisan", "rin", "loan", "kcc"
    )

    fun detect(text: String): DetectedLanguage {
        val clean = text.trim()
        if (clean.isBlank()) return DetectedLanguage.ENGLISH

        // 1. Devanagari script check (Hindi)
        if (clean.any { it.code in 0x0900..0x097F }) {
            return DetectedLanguage.HINDI
        }

        // 2. Hinglish keyword check in Latin script
        val words = clean.lowercase(Locale.ROOT)
            .replace(Regex("[?!.,;:'\"()\\[\\]{}]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        val hinglishMatches = words.count { HINGLISH_KEYWORDS.contains(it) }
        if (hinglishMatches >= 1) {
            return DetectedLanguage.HINGLISH
        }

        // 3. Default to English
        return DetectedLanguage.ENGLISH
    }

    fun isHindiResponsePreferred(text: String, mode: VoiceMode): Boolean {
        return when (mode) {
            VoiceMode.HINDI -> true
            VoiceMode.ENGLISH -> false
            VoiceMode.AUTO -> {
                val detected = detect(text)
                detected == DetectedLanguage.HINDI || detected == DetectedLanguage.HINGLISH
            }
        }
    }
}
