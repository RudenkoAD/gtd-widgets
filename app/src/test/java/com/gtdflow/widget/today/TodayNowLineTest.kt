package com.gtdflow.widget.today

import com.gtdflow.widget.engine.TodayItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Позиция маркера «сейчас» в ленте дня (дефект 3: не зависит от наличия элемента в текущем часе). */
class TodayNowLineTest {

    private fun timed(startMinutes: Int) = TodayItem(
        kind = "event",
        title = "e$startMinutes",
        startMinutes = startMinutes,
        endMinutes = startMinutes + 30,
        file = "f.md",
        line = 1,
        namespace = "Общее",
    )

    private fun allDay() = TodayItem(
        kind = "event",
        title = "весь день",
        allDay = true,
        file = "f.md",
        line = 1,
        namespace = "Общее",
    )

    /** Разреженный день: маркер встаёт МЕЖДУ элементами, хотя в текущем часе элемента нет. */
    @Test
    fun insertsBetweenItemsOnSparseDay() {
        val items = listOf(timed(9 * 60), timed(18 * 60)) // 09:00, 18:00
        assertEquals(1, TodayNowLine.position(items, 14 * 60 + 30)) // 14:30 → между ними
    }

    /** Сейчас раньше всех таймингов → маркер сверху. */
    @Test
    fun aboveAllWhenNowBeforeFirst() {
        val items = listOf(timed(9 * 60), timed(18 * 60))
        assertEquals(0, TodayNowLine.position(items, 5 * 60))
    }

    /** Сейчас позже всех → маркер снизу (индекс = размер списка). */
    @Test
    fun belowAllWhenNowAfterLast() {
        val items = listOf(timed(9 * 60), timed(18 * 60))
        assertEquals(2, TodayNowLine.position(items, 22 * 60))
    }

    /** «Весь день» всегда выше маркера (у него нет времени, ядро ставит его первым). */
    @Test
    fun allDayCountsAboveMarker() {
        val items = listOf(allDay(), timed(9 * 60), timed(18 * 60))
        assertEquals(2, TodayNowLine.position(items, 14 * 60)) // allDay + 09:00 выше, 18:00 ниже
    }

    /** Элемент, начинающийся ровно сейчас, оказывается ПОД маркером (предстоящий). */
    @Test
    fun itemStartingExactlyNowGoesBelow() {
        val items = listOf(timed(9 * 60), timed(14 * 60))
        assertEquals(1, TodayNowLine.position(items, 14 * 60))
    }

    /** Пустой список — позиция 0 (маркер не рисуется, но функция устойчива). */
    @Test
    fun emptyItemsPositionZero() {
        assertEquals(0, TodayNowLine.position(emptyList(), 12 * 60))
    }
}
