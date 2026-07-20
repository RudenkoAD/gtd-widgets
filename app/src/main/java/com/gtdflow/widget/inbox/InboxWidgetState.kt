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

    /** «Общее» — дефолтная метка, когда конфигуратор не задал пространство. */
    const val DEFAULT_NAMESPACE = "Общее"

    fun namespaceOf(prefs: Preferences): String = prefs[NAMESPACE] ?: DEFAULT_NAMESPACE
}
