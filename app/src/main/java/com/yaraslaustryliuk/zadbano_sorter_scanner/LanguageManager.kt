/*
 * © 2025 Yaraslau Stryliuk
 * All rights reserved.
 * Zadbano Sorter Scanner
 */

package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

/**
 * Менеджер управления языком приложения
 */
object LanguageManager {

    private const val PREFS_NAME = "language_settings"
    private const val KEY_LANGUAGE = "selected_language"

    // Поддерживаемые языки
    const val LANGUAGE_POLISH = "pl"
    const val LANGUAGE_RUSSIAN = "ru"

    // Польский по умолчанию
    private const val DEFAULT_LANGUAGE = LANGUAGE_POLISH

    /**
     * Получить текущий выбранный язык
     */
    fun getCurrentLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    /**
     * Установить язык приложения
     */
    fun setLanguage(context: Context, language: String) {
        // Сохраняем выбор в SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language).apply()

        // Применяем язык к контексту
        applyLanguage(context, language)
    }

    /**
     * Применить язык к контексту
     */
    fun applyLanguage(context: Context, language: String) {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
        }

        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    /**
     * Получить контекст с примененным языком
     */
    fun getLanguageContext(context: Context): Context {
        val language = getCurrentLanguage(context)
        val locale = Locale(language)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(android.os.LocaleList(locale))
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Получить название языка для отображения
     */
    fun getLanguageDisplayName(context: Context, language: String): String {
        return when (language) {
            LANGUAGE_POLISH -> context.getString(R.string.polish)
            LANGUAGE_RUSSIAN -> context.getString(R.string.russian)
            else -> language
        }
    }

    /**
     * Инициализация языка при запуске приложения
     */
    fun initializeLanguage(context: Context) {
        val language = getCurrentLanguage(context)
        applyLanguage(context, language)
    }

    /**
     * Получить список поддерживаемых языков
     */
    fun getSupportedLanguages(): List<String> {
        return listOf(LANGUAGE_POLISH, LANGUAGE_RUSSIAN)
    }
}