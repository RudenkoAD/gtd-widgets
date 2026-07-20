package com.gtdflow.widget.agenda

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Ключи per-widget Glance-состояния виджета «Агенда» (PreferencesGlanceStateDefinition).
 *
 * days — сколько ближайших дней показывать (задаёт конфигуратор: 7/14/30). agendaJson —
 * кэш секции agenda (WidgetData.agenda) под это число дней. updated — метка «обновлено
 * HH:mm». error — текст последней ошибки расчёта.
 */
object AgendaWidgetState {
    val DAYS = intPreferencesKey("agenda_days")
    val AGENDA_JSON = stringPreferencesKey("agenda_json")
    val UPDATED = stringPreferencesKey("updated")
    val ERROR = stringPreferencesKey("error")

    /** Дефолт — неделя. */
    const val DEFAULT_DAYS = 7

    /** Допустимые значения конфигуратора. */
    val CHOICES = listOf(7, 14, 30)

    fun daysOf(prefs: Preferences): Int = prefs[DAYS] ?: DEFAULT_DAYS
}
