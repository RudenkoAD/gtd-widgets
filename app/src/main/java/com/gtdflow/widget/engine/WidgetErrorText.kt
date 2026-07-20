package com.gtdflow.widget.engine

/**
 * Приведение любого сбоя расчёта (движок/чтение vault) к короткому
 * человекочитаемому тексту для превью и виджетов.
 *
 * Требование: ЛЮБАЯ ошибка показывается пользователю текстом «Ошибка: …», а не
 * вечной «Загрузка…» и не крешем. Логика вынесена чистой функцией и покрыта JVM.
 */
object WidgetErrorText {

    private const val MAX_LEN = 160

    /** Короткое сообщение из исключения (для строки «Ошибка: …» в виджете). */
    fun forThrowable(t: Throwable): String {
        val raw = t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName
        return clip(raw.trim().replace('\n', ' '))
    }

    /** Готовая строка виджета: «Ошибка: <msg>». */
    fun widgetLine(t: Throwable): String = "Ошибка: ${forThrowable(t)}"

    /**
     * Метка «обновлено …» рядом с кэшированными данными. [updatedHhmm] — время
     * последнего УСПЕШНОГО пересчёта (сбой метку НЕ двигает — см.
     * WidgetService.recordFailure); [stale] — кэш показан, но последний пересчёт
     * упал: добавляем ненавязчивое « · ошибка», чтобы не врать о свежести.
     */
    fun updatedLabel(updatedHhmm: String?, stale: Boolean): String =
        (updatedHhmm ?: "—") + if (stale) " · ошибка" else ""

    private fun clip(s: String): String =
        if (s.length <= MAX_LEN) s else s.take(MAX_LEN - 1) + "…"
}
