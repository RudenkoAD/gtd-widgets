package com.gtdflow.widget.nownext

import com.gtdflow.widget.engine.TodayItem

/**
 * Выбор «что идёт сейчас» и «что следующее» из ленты дня (ЧИСТО, тестируется на JVM).
 *
 * Вход — те же элементы дня, что рисует «Сегодня» (агрегат всех пространств), и снимок
 * времени [nowMinutes] (минуты от полуночи). В хронологии участвуют ТОЛЬКО элементы с
 * временем: «весь день»/недатированные не бывают ни «сейчас», ни «далее» (виджет про
 * часы дня). Никакого Date.now() внутри — время подаёт вызывающий, вывод детерминирован.
 */
object NowNextLogic {

    /**
     * Внутреннее окно отбора для элемента без явного конца (минуты) — см. [effectiveEnd].
     * НЕ отображается: это горизонт «считать текущим», а не заявленный конец элемента.
     */
    const val DEFAULT_DURATION_MINUTES = 30

    /** Результат выбора: текущий (накрывает сейчас) и следующий (ближайший будущий старт). */
    data class NowNext(val current: TodayItem?, val next: TodayItem?)

    /**
     * Конец элемента ДЛЯ ОТБОРА «накрывает ли сейчас» и для границы аларма: явный
     * [TodayItem.endMinutes] либо start + [DEFAULT_DURATION_MINUTES]. Это ВНУТРЕННЕЕ окно, а
     * не отображаемый конец: «Сегодня» длительности не знает и у блока без конца показывает
     * только старт — так же поступает текст «Сейчас» (см. [NowNextText.current]). Через 30 мин
     * виджет просто перестаёт считать элемент текущим, вслух этот конец не утверждая.
     * Недатированный элемент сюда не попадает (его отсекает [select]).
     */
    fun effectiveEnd(item: TodayItem): Int {
        val start = item.startMinutes ?: return 0
        return item.endMinutes ?: (start + DEFAULT_DURATION_MINUTES)
    }

    /**
     * {current, next} среди [items] по [nowMinutes]:
     *  • current — элемент, чей полуинтервал [start, effectiveEnd) накрывает now; при
     *    нескольких перекрывающихся берём с самым ПОЗДНИМ start (самый недавно начавшийся);
     *  • next — элемент с минимальным start, СТРОГО большим now (даже если он раньше конца
     *    current — перекрытия показываем честно).
     * Элементы «весь день»/без времени игнорируются в обоих ролях.
     */
    fun select(items: List<TodayItem>, nowMinutes: Int): NowNext {
        var current: TodayItem? = null
        var currentStart = Int.MIN_VALUE
        var next: TodayItem? = null
        var nextStart = Int.MAX_VALUE
        for (item in items) {
            if (item.allDay) continue
            val start = item.startMinutes ?: continue
            if (start <= nowMinutes && nowMinutes < effectiveEnd(item)) {
                if (start >= currentStart) {
                    current = item
                    currentStart = start
                }
            } else if (start > nowMinutes) {
                if (start < nextStart) {
                    next = item
                    nextStart = start
                }
            }
        }
        return NowNext(current, next)
    }

    /**
     * Ближайшая граница показа СТРОГО после now, где Now/Next обязан перерисоваться:
     * min(конец current, старт next). Обе величины по построению > now. null — если ни
     * current, ни next нет (день пуст или уже закончился) → точный аларм не нужен.
     *
     * Значение может быть ≥ 1440 (элемент без конца близко к полуночи: start+30 за сутки) —
     * планировщик переводит минуты в момент через atStartOfDay().plusMinutes, корректно
     * перекатывая границу за полночь.
     */
    fun nextBoundaryMinutes(items: List<TodayItem>, nowMinutes: Int): Int? {
        val nn = select(items, nowMinutes)
        val currentEnd = nn.current?.let { effectiveEnd(it) }
        val nextStart = nn.next?.startMinutes
        return when {
            currentEnd != null && nextStart != null -> minOf(currentEnd, nextStart)
            else -> currentEnd ?: nextStart
        }
    }

    /** Почему у дня нет ни current, ни next — для честного пустого состояния (см. виджет). */
    enum class EmptyReason {
        /** День вообще без элементов. */
        NO_ITEMS,

        /** Элементы есть, но все «весь день»/недатированные (виджет про часы дня). */
        NO_TIMED,

        /** Таймингом-элементы есть, но все уже прошли. */
        ALL_PAST,
    }

    /**
     * Классификация пустоты для случая, когда [select] вернул {null, null}: отличить «день
     * пуст» от «есть только all-day/недатированные» и от «всё со временем уже прошло». Чистая
     * функция от [items] — зовётся виджетом лишь в ветке без current/next, но самодостаточна
     * (для теста): если таймингом-элемент есть, но current/next пусты, значит он в прошлом.
     */
    fun emptyReason(items: List<TodayItem>): EmptyReason {
        if (items.isEmpty()) return EmptyReason.NO_ITEMS
        val hasTimed = items.any { !it.allDay && it.startMinutes != null }
        return if (hasTimed) EmptyReason.ALL_PAST else EmptyReason.NO_TIMED
    }
}
