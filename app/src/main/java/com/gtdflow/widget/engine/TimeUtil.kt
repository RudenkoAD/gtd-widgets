package com.gtdflow.widget.engine

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Снимок локального времени телефона для входа ядра и форматирование минут в «HH:mm».
 * Ядро само не берёт Date.now()/Intl — детерминированность обеспечивается тем, что
 * дату и минуты-от-полуночи мы фиксируем здесь (java.time, локальная зона).
 */
object TimeUtil {
    private val ISO_DATE: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    /** Локальная дата 'YYYY-MM-DD'. */
    fun todayIso(today: LocalDate = LocalDate.now()): String = today.format(ISO_DATE)

    /** Минуты от полуночи локального времени (0..1439). */
    fun nowMinutes(now: LocalTime = LocalTime.now()): Int = now.hour * 60 + now.minute

    /**
     * Минуты от полуночи → 'HH:mm'. Отрицательные усечены к 00:00; значения ≥ 24ч
     * заворачиваются по модулю суток: конец события за полночь (23:00–01:00)
     * показывается как «01:00», а не бессмысленное «23:59».
     */
    fun minutesToHhmm(minutes: Int): String {
        val m = if (minutes < 0) 0 else minutes % (24 * 60)
        val h = m / 60
        val mm = m % 60
        return "%02d:%02d".format(h, mm)
    }

    /**
     * Отображение времени элемента ленты:
     *  • allDay/оба null → пусто (карточка «весь день» рисует ярлык отдельно);
     *  • только начало → 'HH:mm';
     *  • начало+конец → 'HH:mm–HH:mm' (тире — U+2013).
     */
    fun formatRange(startMinutes: Int?, endMinutes: Int?): String {
        if (startMinutes == null) return ""
        val start = minutesToHhmm(startMinutes)
        if (endMinutes == null) return start
        return "$start–${minutesToHhmm(endMinutes)}"
    }

    /** Час 0..23 для маркера «текущий час» ленты. */
    fun hourOf(minutes: Int): Int = (minutes.coerceIn(0, 24 * 60 - 1)) / 60

    /**
     * РЕДАКТИРУЕМОЕ время для поля правки: обычный дефис '-' (не тире), формат, который
     * принимает buildEditedLine (`HH:mm` | `HH:mm-HH:mm`). Пусто, если времени нет.
     */
    fun editableRange(startMinutes: Int?, endMinutes: Int?): String {
        if (startMinutes == null) return ""
        val start = minutesToHhmm(startMinutes)
        return if (endMinutes == null) start else "$start-${minutesToHhmm(endMinutes)}"
    }
}
