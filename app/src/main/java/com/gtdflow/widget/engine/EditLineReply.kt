package com.gtdflow.widget.engine

import kotlinx.serialization.Serializable

/**
 * Разбор JSON-контракта GtdWidgetCore.buildEditedLine (через мост __gtdBuildEditedLine):
 *   {ok:true,  line:"…"}   → успешная правка, [line] — новая строка файла;
 *   {ok:false, error:"…"} → отказ с кодом ([error], см. [EditErrorText]).
 *
 * Разбор вынесен чистой функцией — граница движка покрывается JVM-тестом без QuickJS.
 */
@Serializable
data class EditLineReply(
    val ok: Boolean = false,
    val line: String? = null,
    val error: String? = null,
) {
    companion object {
        /**
         * Разобрать сырой JSON ответа ядра. Битый/не-JSON ответ трактуем как отказ с
         * псевдо-кодом (движок гарантирует валидный JSON, но граница не должна падать).
         */
        fun parse(raw: String): EditLineReply =
            runCatching { WidgetJson.decodeFromString(serializer(), raw) }
                .getOrElse { EditLineReply(ok = false, error = "bad reply json") }
    }
}

/**
 * Код ошибки buildEditedLine → человекочитаемый текст для шторки/оверлея (ЧИСТО,
 * тестируется на JVM). Коды — из src/widget/widgetEditLine.ts.
 */
object EditErrorText {
    fun humanize(code: String?): String = when (code) {
        null, "" -> "Не удалось сохранить"
        "series-date-not-editable" -> "Дату серии здесь менять нельзя"
        "empty-title" -> "Заголовок не может быть пустым"
        "invalid-title" -> "Недопустимый заголовок"
        "invalid-date" -> "Некорректная дата"
        "invalid-time-range" -> "Некорректное время (HH:mm или HH:mm–HH:mm)"
        "time-without-date" -> "Время без даты — сначала укажите дату"
        "invalid-location" -> "Недопустимое место"
        "invalid-rule" -> "Не удалось изменить правило повтора"
        "not-a-task", "not-a-series" -> "Эту строку нельзя изменить"
        else -> if (code.startsWith("bad edits json")) "Ошибка данных правки" else "Не удалось сохранить: $code"
    }
}
