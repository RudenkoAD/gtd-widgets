package com.gtdflow.widget.reminders

import com.gtdflow.widget.engine.AgendaSection

/**
 * Сбор кандидатов на напоминание из секции агенды (ЧИСТО, тестируется на JVM).
 *
 * Агенда даёт дни с ISO-датой и лентой того же состава, что «Сегодня» (день[0] —
 * сегодня). Берём только элементы С ВРЕМЕНЕМ (startMinutes задан и это не «весь день»):
 * напоминание строится вокруг момента начала. Дата кандидата — дата дня агенды.
 */
object ReminderCandidates {

    fun from(agenda: AgendaSection): List<ReminderCandidate> =
        agenda.days.flatMap { day ->
            day.items.mapNotNull { item ->
                val start = item.startMinutes
                if (item.allDay || start == null) return@mapNotNull null
                ReminderCandidate(
                    date = day.date,
                    startMinutes = start,
                    endMinutes = item.endMinutes,
                    title = item.title,
                    location = item.location?.takeIf { it.isNotBlank() },
                    namespace = item.namespace,
                    file = item.file,
                    line = item.line,
                )
            }
        }
}
