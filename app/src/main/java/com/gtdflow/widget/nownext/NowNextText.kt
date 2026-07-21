package com.gtdflow.widget.nownext

import com.gtdflow.widget.engine.TimeUtil
import com.gtdflow.widget.engine.TodayItem

/**
 * Формирование текста строк «Сейчас → Далее» (ЧИСТО, тестируется на JVM) — вынесено из
 * Glance-виджета, чтобы покрыть форматирование без Android.
 *
 * Принцип честности времени (как «Сегодня»/[TimeUtil.formatRange]): конец элемента вслух
 * НЕ выдумываем. У идущего элемента с ЯВНЫМ концом печатаем «до HH:mm» (пока идёт — важно,
 * когда закончится); без явного конца — только старт, без сфабрикованного «до …».
 * Внутреннее окно отбора [NowNextLogic.effectiveEnd] (start+30) — не отображаемый конец.
 */
object NowNextText {

    /**
     * Первая строка идущего элемента:
     *  • с явным концом → «Сейчас: {title} · до HH:mm»;
     *  • без явного конца → «Сейчас: {title} · HH:mm» (только старт, конец не выдумываем).
     * 📍 место добавляется только на широком виджете ([wide]).
     */
    fun current(item: TodayItem, wide: Boolean): String {
        val time = when {
            item.endMinutes != null -> "до ${TimeUtil.minutesToHhmm(item.endMinutes)}"
            item.startMinutes != null -> TimeUtil.minutesToHhmm(item.startMinutes)
            else -> ""
        }
        val head = if (time.isEmpty()) "Сейчас: ${item.title}" else "Сейчас: ${item.title} · $time"
        return withLocation(head, item.location, wide)
    }

    /**
     * Строка предстоящего элемента: «{prefix}HH:mm {title}» — старт абсолютным временем
     * (например «→ 16:15 Встреча» или «Далее: 16:15 Встреча»). 📍 место — при [withLocation].
     */
    fun next(item: TodayItem, prefix: String, withLocation: Boolean = false): String {
        val start = item.startMinutes
        val time = if (start != null) TimeUtil.minutesToHhmm(start) else ""
        val base = "$prefix$time ${item.title}".trim()
        return withLocation(base, item.location, withLocation)
    }

    /**
     * Ненавязчивая пометка несвежести (как «Сегодня»: см. WidgetErrorText.updatedLabel) —
     * к тексту ПОСЛЕДНЕЙ строки добавляется « · ошибка», когда показан кэш, а последний
     * пересчёт упал. Иначе текст возвращается как есть.
     */
    fun withStale(text: String, stale: Boolean): String = if (stale) "$text · ошибка" else text

    private fun withLocation(base: String, location: String?, show: Boolean): String =
        if (show && !location.isNullOrBlank()) "$base 📍 $location" else base
}
