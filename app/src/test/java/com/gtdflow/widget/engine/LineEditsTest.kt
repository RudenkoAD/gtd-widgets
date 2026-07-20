package com.gtdflow.widget.engine

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Маппинг правок шторки/оверлея в JSON-контракт GtdWidgetCore.buildEditedLine. */
class LineEditsTest {

    private fun obj(json: String) = Json.parseToJsonElement(json).jsonObject

    /** Keep у поля → ключ ОТСУТСТВУЕТ (движок не трогает поле). */
    @Test
    fun keepOmitsKeys() {
        val o = obj(LineEdits.toJson(title = "Привет"))
        assertEquals("Привет", o["title"]!!.jsonPrimitive.content)
        assertFalse(o.containsKey("date"))
        assertFalse(o.containsKey("timeRange"))
        assertFalse(o.containsKey("location"))
    }

    /** Пустой набор правок — пустой объект. */
    @Test
    fun allKeepGivesEmptyObject() {
        assertTrue(obj(LineEdits.toJson()).isEmpty())
    }

    /** Set → значение; Clear → JSON null (снять поле). */
    @Test
    fun setAndClearDistinct() {
        val o = obj(
            LineEdits.toJson(
                title = "Встреча",
                date = FieldEdit.Set("2026-07-20"),
                timeRange = FieldEdit.Set("10:00-11:00"),
                location = FieldEdit.Clear,
            ),
        )
        assertEquals("Встреча", o["title"]!!.jsonPrimitive.content)
        assertEquals("2026-07-20", o["date"]!!.jsonPrimitive.content)
        assertEquals("10:00-11:00", o["timeRange"]!!.jsonPrimitive.content)
        assertEquals(JsonNull, o["location"])
    }

    /** Правка только места (снять) без title — ключ title отсутствует. */
    @Test
    fun clearLocationOnly() {
        val o = obj(LineEdits.toJson(location = FieldEdit.Clear))
        assertFalse(o.containsKey("title"))
        assertEquals(JsonNull, o["location"])
    }

    /** Спецсимволы заголовка корректно экранируются (валидный JSON). */
    @Test
    fun escapesSpecialCharsInTitle() {
        val o = obj(LineEdits.toJson(title = "a \"b\" \\ c"))
        assertEquals("a \"b\" \\ c", o["title"]!!.jsonPrimitive.content)
    }
}
