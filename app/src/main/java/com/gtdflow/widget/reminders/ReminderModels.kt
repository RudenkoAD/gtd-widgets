package com.gtdflow.widget.reminders

/**
 * Кандидат на напоминание — элемент ленты с датой и временем начала. Собирается из
 * секции агенды (см. [ReminderCandidates]); дата берётся из дня агенды, всё остальное —
 * из элемента. Элементы «весь день» и без времени в кандидаты не попадают.
 */
data class ReminderCandidate(
    /** ISO-дата дня элемента 'YYYY-MM-DD'. */
    val date: String,
    /** Минуты от полуночи (0..1439). */
    val startMinutes: Int,
    val endMinutes: Int?,
    val title: String,
    /** Место (строка vault) — null/пусто, если места нет. */
    val location: String?,
    val namespace: String,
    /** Путь файла-источника от корня vault. */
    val file: String,
    /** 1-based номер строки. */
    val line: Int,
)

/**
 * Запланированное напоминание: абсолютное время срабатывания, стабильный ключ/код и
 * готовый текст уведомления. Строится чистым планировщиком [ReminderPlan]; сам показ
 * (AlarmManager + уведомление) — в Android-слое.
 */
data class PlannedReminder(
    /** Стабильный ключ 'file:line:date:startMinutes'. */
    val key: String,
    /** Стабильный неотрицательный requestCode PendingIntent (хэш [key]). */
    val requestCode: Int,
    /** Абсолютное время срабатывания, epoch millis. */
    val triggerAtMillis: Long,
    /** Заголовок уведомления (название события/задачи). */
    val title: String,
    /** Текст уведомления «HH:mm–HH:mm · 📍место · пространство». */
    val text: String,
)
