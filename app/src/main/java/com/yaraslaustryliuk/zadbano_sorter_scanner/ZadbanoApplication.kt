/*
 * © 2025 Yaraslau Stryliuk
 * All rights reserved.
 * Zadbano Sorter Scanner
 */

package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.app.Application

/**
 * Главный класс приложения для инициализации языка
 */
class ZadbanoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализация языка при запуске приложения
        LanguageManager.initializeLanguage(this)
    }
}