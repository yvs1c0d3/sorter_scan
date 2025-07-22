/*
 * © 2025 Yaraslau Stryliuk
 * All rights reserved.
 * Zadbano Sorter Scanner
 */

package com.yaraslaustryliuk.zadbano_sorter_scanner

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Модель данных для отсканированного штрихкода
 *
 * @param code Значение штрихкода
 * @param comment Комментарий пользователя (опционально)
 * @param timestamp Время сканирования
 */
data class BarcodeItem(
    val code: String,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * Возвращает отформатированную дату и время сканирования
     */
    @SuppressLint("SimpleDateFormat")
    fun getFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Возвращает отформатированную дату (только день)
     */
    @SuppressLint("SimpleDateFormat")
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Проверяет, имеет ли элемент комментарий
     */
    fun hasComment(): Boolean = comment.isNotBlank()

    /**
     * Возвращает полное отображение элемента (код + комментарий)
     */
    fun getDisplayText(): String {
        return if (hasComment()) {
            "$code\n$comment"
        } else {
            code
        }
    }

    companion object {
        /**
         * Проверяет, является ли код запрещенным для сканирования
         * Запрещенные префиксы: WAW-MA, WAW-BR, PM
         */
        fun isCodeBlocked(code: String): Boolean {
            val blockedPrefixes = listOf("WAW-MA", "WAW-BR", "PM")
            return blockedPrefixes.any { prefix ->
                code.uppercase().startsWith(prefix.uppercase())
            }
        }

        /**
         * Валидация кода перед добавлением
         */
        fun isValidCode(code: String?): Boolean {
            return !code.isNullOrBlank() && code.trim().length >= 2
        }
    }
}