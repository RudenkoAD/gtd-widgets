package com.gtdflow.widget.nownext

import com.gtdflow.widget.engine.TodayItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Выбор {current, next} и вычисление ближайшей границы показа «Сейчас → Далее»
 * (чистая логика, без Glance/Android). Время — минуты от полуночи.
 */
class NowNextLogicTest {

    private fun at(h: Int, m: Int = 0) = h * 60 + m

    private fun item(
        start: Int?,
        end: Int? = null,
        title: String = "e",
        allDay: Boolean = false,
        location: String? = null,
    ) = TodayItem(
        kind = "event",
        title = title,
        startMinutes = start,
        endMinutes = end,
        allDay = allDay,
        location = location,
        file = "f.md",
        line = 1,
        namespace = "Общее",
    )

    // --- select ---

    /** Пустой список — ни текущего, ни следующего. */
    @Test
    fun emptyHasNeither() {
        val nn = NowNextLogic.select(emptyList(), at(12))
        assertNull(nn.current)
        assertNull(nn.next)
    }

    /** Только будущее: current нет, next — ближайший старт. */
    @Test
    fun onlyFutureGivesNextNoCurrent() {
        val nn = NowNextLogic.select(listOf(item(at(16, 15), at(17))), at(10))
        assertNull(nn.current)
        assertEquals(at(16, 15), nn.next?.startMinutes)
    }

    /** Только прошедшее (уже закончилось) — ни current, ни next. */
    @Test
    fun onlyPastGivesNeither() {
        val nn = NowNextLogic.select(listOf(item(at(8), at(8, 30))), at(10))
        assertNull(nn.current)
        assertNull(nn.next)
    }

    /** Идёт одно — оно current, следующего нет. */
    @Test
    fun singleRunningIsCurrent() {
        val running = item(at(9), at(10), title = "running")
        val nn = NowNextLogic.select(listOf(running), at(9, 30))
        assertSame(running, nn.current)
        assertNull(nn.next)
    }

    /** Перекрытие двух: current — самый недавно начавшийся (поздний start). */
    @Test
    fun overlappingPicksLatestStart() {
        val early = item(at(9), at(11), title = "early")   // 09:00–11:00
        val late = item(at(10), at(10, 30), title = "late") // 10:00–10:30
        val nn = NowNextLogic.select(listOf(early, late), at(10, 15))
        assertSame(late, nn.current)
        assertNull(nn.next)
    }

    /** Элемент без конца накрывает now по правилу end = start + 30. */
    @Test
    fun itemWithoutEndCoversWithDefaultDuration() {
        val open = item(at(16), end = null, title = "open") // 16:00, конец 16:30
        val nn = NowNextLogic.select(listOf(open), at(16, 10))
        assertSame(open, nn.current)
        // За пределами 30 минут — уже не current.
        assertNull(NowNextLogic.select(listOf(open), at(16, 40)).current)
    }

    /** next во время current: старт следующего строго > now, хотя он раньше конца current. */
    @Test
    fun nextDuringCurrentIsHonest() {
        val current = item(at(15), at(17), title = "cur")   // 15:00–17:00
        val soon = item(at(16, 15), at(16, 45), title = "soon") // старт в 16:15
        val nn = NowNextLogic.select(listOf(current, soon), at(16))
        assertSame(current, nn.current)
        assertSame(soon, nn.next)
    }

    /** Элемент, начинающийся ровно сейчас, считается текущим (только что начался). */
    @Test
    fun startingExactlyNowIsCurrent() {
        val nn = NowNextLogic.select(listOf(item(at(14), at(15))), at(14))
        assertEquals(at(14), nn.current?.startMinutes)
        assertNull(nn.next)
    }

    /** «Весь день»/недатированные не участвуют ни в current, ни в next. */
    @Test
    fun allDayAndUndatedIgnored() {
        val items = listOf(
            item(start = null, allDay = true, title = "весь день"),
            item(start = null, title = "без времени"),
            item(at(18), at(19), title = "вечер"),
        )
        val nn = NowNextLogic.select(items, at(12))
        assertNull(nn.current)
        assertEquals("вечер", nn.next?.title)
    }

    // --- nextBoundaryMinutes ---

    /** Пусто — границы нет (аларм не нужен). */
    @Test
    fun boundaryEmptyIsNull() {
        assertNull(NowNextLogic.nextBoundaryMinutes(emptyList(), at(12)))
    }

    /** Только будущее — граница = старт следующего. */
    @Test
    fun boundaryOnlyFutureIsNextStart() {
        val b = NowNextLogic.nextBoundaryMinutes(listOf(item(at(16, 15), at(17))), at(10))
        assertEquals(at(16, 15), b)
    }

    /** Только прошедшее — границы нет. */
    @Test
    fun boundaryOnlyPastIsNull() {
        assertNull(NowNextLogic.nextBoundaryMinutes(listOf(item(at(8), at(8, 30))), at(10)))
    }

    /** Идёт одно без следующего — граница = конец текущего. */
    @Test
    fun boundaryRunningIsCurrentEnd() {
        val b = NowNextLogic.nextBoundaryMinutes(listOf(item(at(9), at(10))), at(9, 30))
        assertEquals(at(10), b)
    }

    /** Элемент без конца — граница = start + 30. */
    @Test
    fun boundaryOpenEndedIsDefaultDuration() {
        val b = NowNextLogic.nextBoundaryMinutes(listOf(item(at(16), end = null)), at(16, 10))
        assertEquals(at(16, 30), b)
    }

    /** next во время current — граница = более ранний старт следующего (перекрытие честно). */
    @Test
    fun boundaryNextDuringCurrentIsNextStart() {
        val items = listOf(item(at(15), at(17)), item(at(16, 15), at(16, 45)))
        assertEquals(at(16, 15), NowNextLogic.nextBoundaryMinutes(items, at(16)))
    }

    /** current заканчивается раньше старта следующего — граница = конец текущего. */
    @Test
    fun boundaryCurrentEndsBeforeNext() {
        val items = listOf(item(at(9), at(10)), item(at(12), at(13)))
        assertEquals(at(10), NowNextLogic.nextBoundaryMinutes(items, at(9, 30)))
    }

    // --- emptyReason (честное пустое состояние) ---

    /** Совсем пустой день — NO_ITEMS («Сегодня событий нет»). */
    @Test
    fun emptyReasonNoItemsWhenDayEmpty() {
        assertEquals(NowNextLogic.EmptyReason.NO_ITEMS, NowNextLogic.emptyReason(emptyList()))
    }

    /** Есть только «весь день»/недатированные — NO_TIMED («Нет событий со временем»). */
    @Test
    fun emptyReasonNoTimedWhenOnlyAllDayOrUndated() {
        val items = listOf(
            item(start = null, allDay = true, title = "весь день"),
            item(start = null, title = "без времени"),
        )
        assertEquals(NowNextLogic.EmptyReason.NO_TIMED, NowNextLogic.emptyReason(items))
    }

    /** Таймингом-элементы есть (но все прошли) — ALL_PAST («Сегодня больше ничего»). */
    @Test
    fun emptyReasonAllPastWhenTimedItemsExist() {
        val items = listOf(
            item(start = null, allDay = true, title = "весь день"),
            item(at(8), at(8, 30), title = "утро"),
        )
        assertEquals(NowNextLogic.EmptyReason.ALL_PAST, NowNextLogic.emptyReason(items))
    }
}
