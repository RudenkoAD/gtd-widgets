package com.gtdflow.widget.engine

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Правка одного поля строки для buildEditedLine. Контракт ядра различает ТРИ исхода
 * на поле, поэтому нельзя обойтись `String?`:
 *  • [Keep]  — ключ НЕ отправляется → движок оставляет поле как есть;
 *  • [Set]   — ключ со строковым значением → установить;
 *  • [Clear] — ключ со значением `null` → СНЯТЬ поле.
 * Разница «нет ключа» и «ключ = null» существенна: null снимает поле, отсутствие —
 * не трогает.
 */
sealed interface FieldEdit {
    data object Keep : FieldEdit
    data class Set(val value: String) : FieldEdit
    data object Clear : FieldEdit
}

/**
 * Сбор JSON-объекта правок для GtdWidgetCore.buildEditedLine (через мост
 * __gtdBuildEditedLine). ЧИСТАЯ функция — маппинг намерений шторки в контракт ядра,
 * тестируется на JVM без движка.
 *
 * title: null → не менять; строка → установить заголовок (пустой заголовок движок
 * отклонит `empty-title`; шторка не должна слать пустой). date/timeRange/location —
 * [FieldEdit]. Для вхождения серии дату слать НЕЛЬЗЯ (движок вернёт
 * `series-date-not-editable`) — вызывающий передаёт [FieldEdit.Keep].
 */
object LineEdits {
    fun toJson(
        title: String? = null,
        date: FieldEdit = FieldEdit.Keep,
        timeRange: FieldEdit = FieldEdit.Keep,
        location: FieldEdit = FieldEdit.Keep,
    ): String {
        val obj: JsonObject = buildJsonObject {
            if (title != null) put("title", title)
            putField("date", date)
            putField("timeRange", timeRange)
            putField("location", location)
        }
        return obj.toString()
    }

    private fun JsonObjectBuilder.putField(key: String, edit: FieldEdit) {
        when (edit) {
            is FieldEdit.Keep -> Unit
            is FieldEdit.Set -> put(key, JsonPrimitive(edit.value))
            is FieldEdit.Clear -> put(key, JsonNull)
        }
    }
}
