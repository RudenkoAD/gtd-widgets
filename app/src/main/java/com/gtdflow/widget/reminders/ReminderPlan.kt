package com.gtdflow.widget.reminders

import com.gtdflow.widget.engine.TimeUtil
import java.time.LocalDate
import java.time.ZoneId

/**
 * Планировщик напоминаний по времени (ЧИСТО, тестируется на JVM).
 *
 * Вход — кандидаты (элементы с датой/временем), упреждение [leadMinutes], снимок
 * времени [nowMillis]/[zone] и горизонт [horizonMillis] (обычно ~36 часов). Выход —
 * список [PlannedReminder]: для каждого кандидата время срабатывания = начало минус
 * упреждение; берём только те, что СТРОГО в будущем и не дальше горизонта. Дубликаты
 * по ключу схлопываются, результат отсортирован по времени срабатывания.
 *
 * java.time доступен на minSdk 26 и в JVM-тестах; всё вычисление детерминировано —
 * дату/время подаёт вызывающий (снимок телефона), «сейчас» тут не берётся.
 */
object ReminderPlan {

    fun plan(
        candidates: List<ReminderCandidate>,
        leadMinutes: Int,
        nowMillis: Long,
        zone: ZoneId,
        horizonMillis: Long,
    ): List<PlannedReminder> {
        val lead = leadMinutes.coerceAtLeast(0)
        val horizonEnd = nowMillis + horizonMillis
        val byKey = LinkedHashMap<String, PlannedReminder>()

        for (c in candidates) {
            val trigger = triggerMillis(c, lead, zone) ?: continue
            if (trigger <= nowMillis || trigger > horizonEnd) continue
            val key = keyOf(c)
            if (byKey.containsKey(key)) continue
            byKey[key] = PlannedReminder(
                key = key,
                requestCode = ReminderRequestCode.of(key),
                triggerAtMillis = trigger,
                title = c.title,
                text = buildText(c),
            )
        }
        return byKey.values.sortedBy { it.triggerAtMillis }
    }

    /** Стабильный ключ элемента: 'file:line:date:startMinutes'. */
    fun keyOf(c: ReminderCandidate): String =
        "${c.file}:${c.line}:${c.date}:${c.startMinutes}"

    /** Текст уведомления: «HH:mm–HH:mm · 📍место · пространство» (пустые части опускаются). */
    fun buildText(c: ReminderCandidate): String {
        val parts = ArrayList<String>(3)
        val range = TimeUtil.formatRange(c.startMinutes, c.endMinutes)
        if (range.isNotEmpty()) parts.add(range)
        val loc = c.location?.trim().orEmpty()
        if (loc.isNotEmpty()) parts.add("📍$loc")
        val ns = c.namespace.trim()
        if (ns.isNotEmpty()) parts.add(ns)
        return parts.joinToString(" · ")
    }

    private fun triggerMillis(c: ReminderCandidate, lead: Int, zone: ZoneId): Long? {
        val date = try {
            LocalDate.parse(c.date)
        } catch (_: Exception) {
            return null
        }
        // startMinutes − lead может уйти в отрицательное (событие в 00:05, упреждение 10 →
        // накануне 23:55). LocalDateTime корректно перекатывает через полночь.
        val fireLocal = date.atStartOfDay().plusMinutes((c.startMinutes - lead).toLong())
        return fireLocal.atZone(zone).toInstant().toEpochMilli()
    }
}
