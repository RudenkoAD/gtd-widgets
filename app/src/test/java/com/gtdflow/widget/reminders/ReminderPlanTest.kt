package com.gtdflow.widget.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Планировщик напоминаний по времени: отбор будущих в пределах горизонта, упреждение
 * (в т.ч. перенос через полночь), дедуп по ключу, сортировка, текст уведомления.
 */
class ReminderPlanTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val horizon36h = 36L * 60L * 60L * 1000L

    private fun millis(y: Int, mo: Int, d: Int, h: Int, mi: Int): Long =
        LocalDateTime.of(y, mo, d, h, mi).atZone(zone).toInstant().toEpochMilli()

    private fun candidate(
        date: String,
        start: Int,
        end: Int? = null,
        title: String = "Созвон",
        location: String? = null,
        namespace: String = "Общее",
        file: String = "f.md",
        line: Int = 1,
    ) = ReminderCandidate(date, start, end, title, location, namespace, file, line)

    @Test
    fun schedulesFutureReminderWithLead() {
        val now = millis(2026, 7, 21, 9, 0)
        val plan = ReminderPlan.plan(
            listOf(candidate("2026-07-21", start = 600, end = 630)),
            leadMinutes = 10,
            nowMillis = now,
            zone = zone,
            horizonMillis = horizon36h,
        )
        assertEquals(1, plan.size)
        val r = plan[0]
        assertEquals(millis(2026, 7, 21, 9, 50), r.triggerAtMillis) // 10:00 − 10 мин
        assertEquals("Созвон", r.title)
        assertEquals("10:00–10:30 · Общее", r.text) // 10:00–10:30 · Общее
        assertEquals("f.md:1:2026-07-21:600", r.key)
        assertEquals(ReminderRequestCode.of("f.md:1:2026-07-21:600"), r.requestCode)
    }

    @Test
    fun leadZeroFiresAtStart() {
        val now = millis(2026, 7, 21, 9, 0)
        val plan = ReminderPlan.plan(
            listOf(candidate("2026-07-21", start = 600)),
            leadMinutes = 0, nowMillis = now, zone = zone, horizonMillis = horizon36h,
        )
        assertEquals(millis(2026, 7, 21, 10, 0), plan.single().triggerAtMillis)
    }

    @Test
    fun excludesPastReminder() {
        val now = millis(2026, 7, 21, 9, 0)
        val plan = ReminderPlan.plan(
            listOf(candidate("2026-07-21", start = 480)), // 08:00 − уже прошло
            leadMinutes = 10, nowMillis = now, zone = zone, horizonMillis = horizon36h,
        )
        assertTrue(plan.isEmpty())
    }

    @Test
    fun excludesBeyondHorizonKeepsWithin() {
        val now = millis(2026, 7, 21, 9, 0) // горизонт до 2026-07-22 21:00
        val plan = ReminderPlan.plan(
            listOf(
                candidate("2026-07-22", start = 600, file = "in.md"),  // 07-22 09:50 — в пределах
                candidate("2026-07-23", start = 600, file = "out.md"), // 07-23 09:50 — за горизонтом
            ),
            leadMinutes = 10, nowMillis = now, zone = zone, horizonMillis = horizon36h,
        )
        assertEquals(1, plan.size)
        assertEquals("in.md:1:2026-07-22:600", plan[0].key)
    }

    @Test
    fun dedupesSameKey() {
        val now = millis(2026, 7, 21, 9, 0)
        val plan = ReminderPlan.plan(
            listOf(
                candidate("2026-07-21", start = 600),
                candidate("2026-07-21", start = 600), // тот же file:line:date:start
            ),
            leadMinutes = 10, nowMillis = now, zone = zone, horizonMillis = horizon36h,
        )
        assertEquals(1, plan.size)
    }

    @Test
    fun sortedByTriggerAscending() {
        val now = millis(2026, 7, 21, 6, 0)
        val plan = ReminderPlan.plan(
            listOf(
                candidate("2026-07-21", start = 1080, file = "late.md"),  // 18:00
                candidate("2026-07-21", start = 600, file = "early.md"),  // 10:00
            ),
            leadMinutes = 10, nowMillis = now, zone = zone, horizonMillis = horizon36h,
        )
        assertEquals(listOf("early.md:1:2026-07-21:600", "late.md:1:2026-07-21:1080"), plan.map { it.key })
        assertTrue(plan[0].triggerAtMillis < plan[1].triggerAtMillis)
    }

    @Test
    fun leadCrossesMidnight() {
        val now = millis(2026, 7, 21, 9, 0)
        val plan = ReminderPlan.plan(
            listOf(candidate("2026-07-22", start = 5)), // 00:05 22-го, упреждение 10 → 23:55 21-го
            leadMinutes = 10, nowMillis = now, zone = zone, horizonMillis = horizon36h,
        )
        assertEquals(millis(2026, 7, 21, 23, 55), plan.single().triggerAtMillis)
    }

    @Test
    fun buildTextWithLocation() {
        val text = ReminderPlan.buildText(
            candidate("2026-07-21", start = 600, location = "Кафе", namespace = "Работа"),
        )
        assertEquals("10:00 · 📍Кафе · Работа", text)
    }
}
