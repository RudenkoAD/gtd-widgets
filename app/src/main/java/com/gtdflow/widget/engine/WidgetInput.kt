package com.gtdflow.widget.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Вход GtdWidgetCore.computeWidgetData (зеркало WidgetInput из widgetCore.ts).
 * files — карта «путь-от-корня-vault → содержимое .md». dataJson — сырой data.json
 * плагина или null. todayIso/nowMinutes — локальные дата/время телефона. Всё
 * вычисление дат делает JS-ядро; Kotlin лишь передаёт снимок времени.
 */
@Serializable
data class WidgetInput(
    val files: Map<String, String>,
    val dataJson: String?,
    val todayIso: String,
    val nowMinutes: Int,
    val inboxNamespace: String?,
) {
    fun toJson(): String = WidgetJson.encodeToString(this)
}
