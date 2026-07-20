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
    /** Секция агенды (список дней от сегодня); пуста, если agendaDays не запрашивался. */
    val agenda: AgendaSection = AgendaSection(),
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
    /** "event" | "task" (совместимость). */
    val kind: String,
    /** Уточнённый вид: "single-event" | "series-occurrence" | "task". Может быть пуст
     *  на старом контракте — тогда полагаемся на [kind]. */
    val itemKind: String = "",
    val title: String,
    val startMinutes: Int? = null,
    val endMinutes: Int? = null,
    val allDay: Boolean = false,
    val location: String? = null,
    val file: String,
    /** 1-based. */
    val line: Int,
    val namespace: String,
    /** Исходная строка файла — источник правок шторки (buildEditedLine) и точный якорь записи. */
    val rawLine: String = "",
    /** Текст правила 🔁 для вхождения серии; null для одноразового события/задачи. */
    val recurrenceText: String? = null,
) {
    val isEvent: Boolean get() = kind == "event"

    /** Вхождение повторяющейся серии (правка правит всю серию, дату двигать нельзя). */
    val isSeriesOccurrence: Boolean get() = itemKind == "series-occurrence"
}

/** Секция агенды: дни от сегодня включительно (число задаёт конфиг виджета). */
@Serializable
data class AgendaSection(
    val days: List<AgendaDay> = emptyList(),
)

/** Один день агенды: ISO-дата и лента того же состава/сортировки, что today.items. */
@Serializable
data class AgendaDay(
    val date: String,
    val items: List<TodayItem> = emptyList(),
)

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
    /** Пространство файла-источника (метка) — показывается в агрегате «Все». */
    val namespace: String = "",
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
