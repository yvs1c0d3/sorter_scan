/*
 * © 2025 Yaraslau Stryliuk
 * All rights reserved.
 * Zadbano Sorter Scanner
 */

package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Activity для настройки фильтров штрихкодов
 */
class FilterSettingsActivity : AppCompatActivity() {

    private lateinit var filterEnabledSwitch: SwitchMaterial
    private lateinit var filtersRecyclerView: RecyclerView
    private lateinit var addFilterButton: MaterialButton
    private lateinit var resetFiltersButton: MaterialButton
    private lateinit var filterStatusText: MaterialTextView
    private lateinit var filterAdapter: FilterAdapter

    private val blockedPrefixes = mutableListOf<String>()

    companion object {
        private const val PREFS_NAME = "filter_settings"
        private const val KEY_FILTER_ENABLED = "filter_enabled"
        private const val KEY_BLOCKED_PREFIXES = "blocked_prefixes"

        // Дефолтные фильтры
        val DEFAULT_BLOCKED_PREFIXES = listOf("WAW-MA", "WAW-BR", "PM")

        /**
         * Получить текущие настройки фильтра
         */
        fun getFilterSettings(context: Context): FilterSettings {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val isEnabled = prefs.getBoolean(KEY_FILTER_ENABLED, true)
            val json = prefs.getString(KEY_BLOCKED_PREFIXES, null)

            val prefixes = if (json != null) {
                try {
                    val type = object : TypeToken<List<String>>() {}.type
                    Gson().fromJson<List<String>>(json, type)
                } catch (e: Exception) {
                    DEFAULT_BLOCKED_PREFIXES
                }
            } else {
                DEFAULT_BLOCKED_PREFIXES
            }

            return FilterSettings(isEnabled, prefixes)
        }

        /**
         * Сохранить настройки фильтра
         */
        fun saveFilterSettings(context: Context, settings: FilterSettings) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            editor.putBoolean(KEY_FILTER_ENABLED, settings.isEnabled)
            editor.putString(KEY_BLOCKED_PREFIXES, Gson().toJson(settings.blockedPrefixes))
            editor.apply()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase?.let { LanguageManager.getLanguageContext(it) })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_filter_settings)

        initializeViews()
        setupToolbar()
        loadCurrentSettings()
        setupRecyclerView()
        setupClickListeners()
        updateUI()
    }

    private fun initializeViews() {
        filterEnabledSwitch = findViewById(R.id.filterEnabledSwitch)
        filtersRecyclerView = findViewById(R.id.filtersRecyclerView)
        addFilterButton = findViewById(R.id.addFilterButton)
        resetFiltersButton = findViewById(R.id.resetFiltersButton)
        filterStatusText = findViewById(R.id.filterStatusText)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Настройки фильтра"
    }

    private fun loadCurrentSettings() {
        val settings = getFilterSettings(this)
        filterEnabledSwitch.isChecked = settings.isEnabled
        blockedPrefixes.clear()
        blockedPrefixes.addAll(settings.blockedPrefixes)
    }

    private fun setupRecyclerView() {
        filterAdapter = FilterAdapter(
            prefixes = blockedPrefixes,
            onDeleteClick = { position ->
                deleteFilter(position)
            }
        )
        filtersRecyclerView.layoutManager = LinearLayoutManager(this)
        filtersRecyclerView.adapter = filterAdapter
    }

    private fun setupClickListeners() {
        filterEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            updateUI()
            saveCurrentSettings()
        }

        addFilterButton.setOnClickListener {
            showAddFilterDialog()
        }

        resetFiltersButton.setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun updateUI() {
        val isEnabled = filterEnabledSwitch.isChecked

        // Обновляем статус
        filterStatusText.text = if (isEnabled) {
            "🟢 Фильтр включен (${blockedPrefixes.size} правил)"
        } else {
            "🔴 Фильтр выключен"
        }

        // Включаем/выключаем кнопки
        addFilterButton.isEnabled = isEnabled
        resetFiltersButton.isEnabled = isEnabled
        filtersRecyclerView.alpha = if (isEnabled) 1.0f else 0.5f
    }

    private fun showAddFilterDialog() {
        val editText = TextInputEditText(this).apply {
            hint = "Введите префикс для блокировки"
            setPadding(48, 48, 48, 48)
        }

        val container = LinearLayout(this).apply {
            setPadding(48, 24, 48, 24)
            addView(editText)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Добавить фильтр")
            .setMessage("Коды, начинающиеся с этого префикса, будут заблокированы для сканирования")
            .setView(container)
            .setPositiveButton("Добавить") { _, _ ->
                val prefix = editText.text.toString().trim().uppercase()
                if (prefix.isNotEmpty()) {
                    addFilter(prefix)
                } else {
                    Toast.makeText(this, "Префикс не может быть пустым", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addFilter(prefix: String) {
        if (!blockedPrefixes.contains(prefix)) {
            blockedPrefixes.add(prefix)
            filterAdapter.notifyItemInserted(blockedPrefixes.size - 1)
            saveCurrentSettings()
            updateUI()
            Toast.makeText(this, "Фильтр \"$prefix\" добавлен", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Фильтр \"$prefix\" уже существует", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteFilter(position: Int) {
        if (position >= 0 && position < blockedPrefixes.size) {
            val deletedPrefix = blockedPrefixes[position]
            blockedPrefixes.removeAt(position)
            filterAdapter.notifyItemRemoved(position)
            saveCurrentSettings()
            updateUI()
            Toast.makeText(this, "Фильтр \"$deletedPrefix\" удален", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showResetConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Сброс фильтров")
            .setMessage("Вы уверены, что хотите сбросить все фильтры до значений по умолчанию?")
            .setPositiveButton("Сбросить") { _, _ ->
                resetToDefaults()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun resetToDefaults() {
        blockedPrefixes.clear()
        blockedPrefixes.addAll(DEFAULT_BLOCKED_PREFIXES)
        filterAdapter.notifyDataSetChanged()
        saveCurrentSettings()
        updateUI()
        Toast.makeText(this, "Фильтры сброшены до значений по умолчанию", Toast.LENGTH_SHORT).show()
    }

    private fun saveCurrentSettings() {
        val settings = FilterSettings(
            isEnabled = filterEnabledSwitch.isChecked,
            blockedPrefixes = blockedPrefixes.toList()
        )
        saveFilterSettings(this, settings)
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

/**
 * Модель настроек фильтра
 */
data class FilterSettings(
    val isEnabled: Boolean,
    val blockedPrefixes: List<String>
)