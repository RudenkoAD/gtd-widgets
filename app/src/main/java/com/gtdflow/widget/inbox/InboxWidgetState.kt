package com.gtdflow.widget.inbox

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Ключи per-widget Glance-состояния виджета «Входящие» (PreferencesGlanceStateDefinition).
 *
 * namespace — выбранное в конфигураторе пространство (аргумент виджета: имя / «Общее» /
 * «Все»). inboxJson — кэш секции inbox (WidgetData.inbox) для этого пространства.
 * updated — метка «обновлено HH:mm».
 */
object InboxWidgetState {
    val NAMESPACE = stringPreferencesKey("namespace")
    val INBOX_JSON = stringPreferencesKey("inbox_json")
    val UPDATED = stringPreferencesKey("updated")

    /** Текст последней ошибки расчёта для этого виджета (отсутствует — успех). */
    val ERROR = stringPreferencesKey("error")

    /**
     * Транзиентная заметка интеракции («Файл изменился — обновите виджет» при промахе
     * чекбокса). В отличие от [ERROR] не заменяет список: рисуется строкой над ним и
     * НЕ липнет — следующий успешный пересчёт её снимает (WidgetService.refresh).
     */
    val NOTICE = stringPreferencesKey("notice")

    /** «Общее» — дефолтная метка, когда конфигуратор не задал пространство. */
    const val DEFAULT_NAMESPACE = "Общее"

    /** «Все» — метка агрегата всех пространств (дефолт нового экземпляра). */
    const val ALL_NAMESPACE = "Все"

    fun namespaceOf(prefs: Preferences): String = prefs[NAMESPACE] ?: DEFAULT_NAMESPACE

    /** Показывать ли метку пространства у каждой строки (актуально только для «Все»). */
    fun isAggregate(namespace: String): Boolean = namespace == ALL_NAMESPACE
}
