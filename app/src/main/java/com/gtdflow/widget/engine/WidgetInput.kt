package com.gtdflow.widget.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString

/**
 * Вход GtdWidgetCore.computeWidgetData (зеркало WidgetInput из widgetCore.ts).
 * files — карта «путь-от-корня-vault → содержимое .md». dataJson — сырой data.json
 * плагина или null. todayIso/nowMinutes — локальные дата/время телефона. Всё
 * вычисление дат делает JS-ядро; Kotlin лишь передаёт снимок времени.
 *
 * agendaDays — сколько дней агенды считать от todayIso включительно (0/null — не
 * считать; ядро клампит к [0,30]). null-значение опускается при сериализации
 * (encodeDefaults=false у WidgetJson не задан, но kotlinx по умолчанию опускает
 * null-поля с дефолтом — ядро трактует отсутствие как 0).
 */
@Serializable
data class WidgetInput(
    val files: Map<String, String>,
    val dataJson: String?,
    val todayIso: String,
    val nowMinutes: Int,
    val inboxNamespace: String?,
    val agendaDays: Int? = null,
) {
    fun toJson(): String = WidgetJson.encodeToString(this)
}
