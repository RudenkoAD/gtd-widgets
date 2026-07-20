package com.gtdflow.widget.agenda

import java.time.LocalDate

/**
 * Заголовок дня агенды на русском БЕЗ зависимости от локали/ICU устройства (движок
 * Intl не имеет, а java.time.format с Locale тянет данные ОС — на разных прошивках
 * различается). Названия зашиты массивами, вычисление — из [LocalDate]. ЧИСТО,
 * тестируется на JVM.
 *
 * Формат: «Пн, 21 июля». Если задан [todayIso]: день == сегодня → «Сегодня, 21 июля»,
 * день == завтра → «Завтра, 22 июля» (относительные метки читаются в ленте быстрее).
 * Битая дата возвращается как есть (агенда не должна падать из-за одного дня).
 */
object AgendaDayHeader {

    // dayOfWeek.value: 1=понедельник … 7=воскресенье.
    private val DOW = arrayOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    // monthValue: 1..12; родительный падеж («21 июля»).
    private val MONTHS_GENITIVE = arrayOf(
        "января", "февраля", "марта", "апреля", "мая", "июня",
        "июля", "августа", "сентября", "октября", "ноября", "декабря",
    )

    fun format(dateIso: String, todayIso: String? = null): String {
        val date = runCatching { LocalDate.parse(dateIso) }.getOrNull() ?: return dateIso
        val dayMonth = "${date.dayOfMonth} ${MONTHS_GENITIVE[date.monthValue - 1]}"
        val prefix = relativePrefix(date, todayIso) ?: DOW[date.dayOfWeek.value - 1]
        return "$prefix, $dayMonth"
    }

    /** «Сегодня»/«Завтра», если [todayIso] валиден и день попадает; иначе null (день недели). */
    private fun relativePrefix(date: LocalDate, todayIso: String?): String? {
        val today = todayIso?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
        return when (date) {
            today -> "Сегодня"
            today.plusDays(1) -> "Завтра"
            else -> null
        }
    }
}
