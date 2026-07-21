package com.gtdflow.widget.capture

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gtdflow.widget.inbox.InboxWidgetState

/**
 * Ключ per-widget Glance-состояния виджета «Захват»: выбранное в конфигураторе
 * пространство назначения (имя / «Общее» / «Все»). Дефолт нового экземпляра — «Все»
 * (захват при «Все»/«Общее» уходит в «Общее», см. CaptureNamespace/CaptureService).
 * Данных vault виджет не держит — это чистая кнопка.
 */
object CaptureWidgetState {
    val NAMESPACE = stringPreferencesKey("namespace")

    fun namespaceOf(prefs: Preferences): String = prefs[NAMESPACE] ?: InboxWidgetState.ALL_NAMESPACE
}
