package com.gtdflow.widget.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Модели JSON-вывода GtdWidgetCore.computeWidgetData (зеркало WidgetData из
 * src/widget/widgetCore.ts). Разбираются kotlinx.serialization; лишние поля
 * игнорируются (ignoreUnknownKeys) на случай расширения контракта в JS-зоне.
 *
 * Именование в JS — camelCase; @SerialName задаёт точные ключи, чтобы Kotlin-стиль
 * не расходился с контрактом.
 */
@Serializable
data class WidgetData(
    val today: TodaySection,
    val inbox: InboxSection,
    val namespaces: List<NamespaceDef> = emptyList(),
    val errors: List<String> = emptyList(),
)

@Serializable
data class TodaySection(
    val date: String,
    val items: List<TodayItem> = emptyList(),
    val generatedAt: String,
)

@Serializable
data class TodayItem(
    /** "event" | "task". */
    val kind: String,
    val title: String,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val allDay: Boolean = false,
    val location: String? = null,
    val file: String,
    /** 1-based. */
    val line: Int,
    val namespace: String,
) {
    val isEvent: Boolean get() = kind == "event"
}

@Serializable
data class InboxSection(
    /** Человекочитаемая метка активного пространства («Общее»/«Все»/имя). */
    val namespace: String,
    val items: List<InboxItem> = emptyList(),
)

@Serializable
data class InboxItem(
    val title: String,
    val file: String,
    /** 1-based. */
    val line: Int,
    val id: String? = null,
    val location: String? = null,
)

/** Пользовательское пространство (для конфигуратора виджета входящих). */
@Serializable
data class NamespaceDef(
    val name: String,
    val root: String,
)

/** Единый JSON-парсер модуля: терпим к неизвестным ключам, компактный вывод. */
val WidgetJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
