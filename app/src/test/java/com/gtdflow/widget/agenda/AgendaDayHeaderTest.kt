package com.gtdflow.widget.agenda

import org.junit.Assert.assertEquals
import org.junit.Test

/** Форматирование заголовка дня агенды (русские названия без зависимости от локали ОС). */
class AgendaDayHeaderTest {

    @Test
    fun formatsWeekdayAndGenitiveMonth() {
        // 2026-07-20 — понедельник; месяц в родительном падеже.
        assertEquals("Пн, 20 июля", AgendaDayHeader.format("2026-07-20"))
        assertEquals("Ср, 22 июля", AgendaDayHeader.format("2026-07-22"))
        assertEquals("Чт, 1 января", AgendaDayHeader.format("2026-01-01"))
        assertEquals("Чт, 31 декабря", AgendaDayHeader.format("2026-12-31"))
    }

    @Test
    fun usesRelativeLabelsWhenTodayGiven() {
        val today = "2026-07-20"
        assertEquals("Сегодня, 20 июля", AgendaDayHeader.format("2026-07-20", today))
        assertEquals("Завтра, 21 июля", AgendaDayHeader.format("2026-07-21", today))
        // послезавтра и дальше — обычный день недели
        assertEquals("Ср, 22 июля", AgendaDayHeader.format("2026-07-22", today))
    }

    @Test
    fun brokenDateReturnedAsIs() {
        assertEquals("не дата", AgendaDayHeader.format("не дата"))
        assertEquals("2026-13-40", AgendaDayHeader.format("2026-13-40"))
        // битый todayIso не мешает: падаем к дню недели
        assertEquals("Пн, 20 июля", AgendaDayHeader.format("2026-07-20", "мусор"))
    }
}
