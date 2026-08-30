package com.krishimitra.app.domain.language

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val locale: Locale) {
    HINDI("hi", "हिन्दी", Locale("hi", "IN")),
    ENGLISH("en", "English", Locale("en", "IN"))
}

class LanguageManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "krishi_language_prefs"
        private const val KEY_SELECTED_LANG = "selected_language"

        @Volatile
        private var instance: LanguageManager? = null

        fun getInstance(context: Context): LanguageManager {
            return instance ?: synchronized(this) {
                instance ?: LanguageManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentLanguage: MutableStateFlow<AppLanguage>

    val currentLanguage: StateFlow<AppLanguage>

    init {
        val savedCode = prefs.getString(KEY_SELECTED_LANG, AppLanguage.HINDI.code)
        val initialLang = if (savedCode == AppLanguage.ENGLISH.code) AppLanguage.ENGLISH else AppLanguage.HINDI
        _currentLanguage = MutableStateFlow(initialLang)
        currentLanguage = _currentLanguage.asStateFlow()
        applyLocaleToConfiguration(context, initialLang.locale)
    }

    fun setLanguage(language: AppLanguage) {
        if (_currentLanguage.value == language) return

        prefs.edit().putString(KEY_SELECTED_LANG, language.code).apply()
        _currentLanguage.value = language
        applyLocaleToConfiguration(context, language.locale)
    }

    fun toggleLanguage() {
        val nextLang = if (_currentLanguage.value == AppLanguage.HINDI) AppLanguage.ENGLISH else AppLanguage.HINDI
        setLanguage(nextLang)
    }

    fun applyLocaleToConfiguration(ctx: Context, locale: Locale): Context {
        Locale.setDefault(locale)
        val config = Configuration(ctx.resources.configuration)
        config.setLocale(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)
        config.setLocales(localeList)

        return ctx.createConfigurationContext(config)
    }
}
