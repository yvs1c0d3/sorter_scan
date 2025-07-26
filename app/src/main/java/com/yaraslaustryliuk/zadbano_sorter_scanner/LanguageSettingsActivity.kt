/*
 * © 2025 Yaraslau Stryliuk
 * All rights reserved.
 * Zadbano Sorter Scanner
 */

package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.google.android.material.card.MaterialCardView

/**
 * Activity для настройки языка приложения
 */
class LanguageSettingsActivity : AppCompatActivity() {

    private lateinit var currentLanguageText: MaterialTextView
    private lateinit var languageCard: MaterialCardView

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LanguageManager.getLanguageContext(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_settings)

        initializeViews()
        setupToolbar()
        setupClickListeners()
        updateCurrentLanguageDisplay()
    }

    private fun initializeViews() {
        currentLanguageText = findViewById(R.id.currentLanguageText)
        languageCard = findViewById(R.id.languageCard)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.language_settings)
    }

    private fun setupClickListeners() {
        languageCard.setOnClickListener {
            showLanguageSelectionDialog()
        }
    }

    private fun updateCurrentLanguageDisplay() {
        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        val languageName = LanguageManager.getLanguageDisplayName(this, currentLanguage)
        currentLanguageText.text = languageName
    }

    private fun showLanguageSelectionDialog() {
        val languages = LanguageManager.getSupportedLanguages()
        val languageNames = languages.map {
            LanguageManager.getLanguageDisplayName(this, it)
        }.toTypedArray()

        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        val currentIndex = languages.indexOf(currentLanguage)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.change_language))
            .setSingleChoiceItems(languageNames, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                if (selectedLanguage != currentLanguage) {
                    changeLanguage(selectedLanguage)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun changeLanguage(language: String) {
        // Сохраняем выбранный язык
        LanguageManager.setLanguage(this, language)

        // Показываем сообщение об изменении
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.language_changed))
            .setMessage(getString(R.string.restart_required))
            .setPositiveButton("OK") { _, _ ->
                // Перезапускаем приложение для применения изменений
                restartApp()
            }
            .setCancelable(false)
            .show()
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}