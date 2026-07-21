package com.gtdflow.widget.nownext

import com.gtdflow.widget.engine.TodayItem
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Форматирование строк «Сейчас → Далее» (чистое, без Glance/Android). Ключевое: конец
 * элемента вслух НЕ выдумываем — у идущего с явным концом «до HH:mm», без конца — только старт.
 */
class NowNextTextTest {

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    private fun item(
        start: Int?,
        end: Int? = null,
        title: String = "Позвонить",
        location: String? = null,
    ) = TodayItem(
        kind = "event",
        title = title,
        startMinutes = start,
        endMinutes = end,
        allDay = false,
        location = location,
        file = "f.md",
        line = 1,
        namespace = "Общее",
    )

    // --- current ---

    /** Идущий С ЯВНЫМ концом: «Сейчас: … · до HH:mm». */
    @Test
    fun currentWithEndShowsUntil() {
        assertEquals(
            "Сейчас: Позвонить · до 16:00",
            NowNextText.current(item(at(15), at(16)), wide = false),
        )
    }

    /** Идущий БЕЗ явного конца: только старт, без сфабрикованного «до …». */
    @Test
    fun currentWithoutEndShowsStartOnly() {
        assertEquals(
            "Сейчас: Позвонить · 16:00",
            NowNextText.current(item(at(16), end = null), wide = false),
        )
    }

    /** На широком виджете к строке добавляется 📍 место. */
    @Test
    fun currentWideAppendsLocation() {
        assertEquals(
            "Сейчас: Позвонить · 16:00 📍 Дом",
            NowNextText.current(item(at(16), end = null, location = "Дом"), wide = true),
        )
    }

    /** На узком (3×1) место скрыто. */
    @Test
    fun currentNarrowHidesLocation() {
        assertEquals(
            "Сейчас: Позвонить · 16:00",
            NowNextText.current(item(at(16), end = null, location = "Дом"), wide = false),
        )
    }

    // --- next ---

    /** Предстоящее: «{prefix}HH:mm {title}» стартом. */
    @Test
    fun nextShowsPrefixStartTitle() {
        assertEquals(
            "→ 16:15 Позвонить",
            NowNextText.next(item(at(16, 15)), prefix = "→ "),
        )
    }

    /** Место у предстоящего — только когда явно попросили (широкий виджет). */
    @Test
    fun nextAppendsLocationOnlyWhenAsked() {
        val ev = item(at(16, 15), location = "Офис")
        assertEquals("Далее: 16:15 Позвонить", NowNextText.next(ev, prefix = "Далее: "))
        assertEquals(
            "Далее: 16:15 Позвонить 📍 Офис",
            NowNextText.next(ev, prefix = "Далее: ", withLocation = true),
        )
    }

    // --- withStale ---

    /** Несвежесть добавляет ненавязчивый « · ошибка» к строке. */
    @Test
    fun withStaleAppendsErrorSuffix() {
        assertEquals("→ 16:15 Позвонить · ошибка", NowNextText.withStale("→ 16:15 Позвонить", stale = true))
    }

    /** Свежие данные — строка без изменений. */
    @Test
    fun withStaleNoopWhenFresh() {
        assertEquals("→ 16:15 Позвонить", NowNextText.withStale("→ 16:15 Позвонить", stale = false))
    }
}
