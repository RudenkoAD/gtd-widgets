package com.gtdflow.widget.reminders

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** Отдельный DataStore настроек напоминаний (не смешиваем с кэшем виджетов). */
private val Context.reminderDataStore: DataStore<Preferences> by
    preferencesDataStore(name = "gtd_reminders")

/**
 * Настройки и служебное состояние напоминаний.
 *
 * Пользовательские тумблеры: напоминания по времени (+ упреждение LEAD) и по месту.
 * Служебное: множество requestCode запланированных будильников — по нему
 * перепланирование wholesale отменяет «ушедшие», а BOOT-перевзвод сверяется с планом.
 */
object ReminderStore {

    private val KEY_TIME_ENABLED = booleanPreferencesKey("time_enabled")
    private val KEY_LEAD = intPreferencesKey("lead_minutes")
    private val KEY_PLACE_ENABLED = booleanPreferencesKey("place_enabled")
    private val KEY_SCHEDULED = stringSetPreferencesKey("scheduled_codes")

    /** Допустимые значения упреждения (минуты). */
    val LEAD_CHOICES = listOf(0, 5, 10, 30)
    const val DEFAULT_LEAD = 10

    data class Prefs(
        val timeEnabled: Boolean,
        val leadMinutes: Int,
        val placeEnabled: Boolean,
    ) {
        val anyEnabled: Boolean get() = timeEnabled || placeEnabled
    }

    fun prefsFlow(context: Context): Flow<Prefs> =
        context.reminderDataStore.data.map { p ->
            Prefs(
                timeEnabled = p[KEY_TIME_ENABLED] ?: false,
                leadMinutes = normalizeLead(p[KEY_LEAD] ?: DEFAULT_LEAD),
                placeEnabled = p[KEY_PLACE_ENABLED] ?: false,
            )
        }

    suspend fun prefs(context: Context): Prefs = prefsFlow(context).first()

    suspend fun setTimeEnabled(context: Context, enabled: Boolean) {
        context.reminderDataStore.edit { it[KEY_TIME_ENABLED] = enabled }
    }

    suspend fun setLead(context: Context, minutes: Int) {
        context.reminderDataStore.edit { it[KEY_LEAD] = normalizeLead(minutes) }
    }

    suspend fun setPlaceEnabled(context: Context, enabled: Boolean) {
        context.reminderDataStore.edit { it[KEY_PLACE_ENABLED] = enabled }
    }

    /** Сохранённые requestCode запланированных будильников. */
    suspend fun scheduledCodes(context: Context): Set<Int> =
        context.reminderDataStore.data.first()[KEY_SCHEDULED]
            ?.mapNotNull { it.toIntOrNull() }?.toSet()
            ?: emptySet()

    suspend fun saveScheduledCodes(context: Context, codes: Set<Int>) {
        context.reminderDataStore.edit { p ->
            p[KEY_SCHEDULED] = codes.map { it.toString() }.toSet()
        }
    }

    private fun normalizeLead(value: Int): Int =
        if (value in LEAD_CHOICES) value else DEFAULT_LEAD
}
