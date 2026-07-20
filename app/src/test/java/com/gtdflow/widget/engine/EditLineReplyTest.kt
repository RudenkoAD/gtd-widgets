package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Разбор ответа buildEditedLine и перевод кодов ошибок в текст. */
class EditLineReplyTest {

    @Test
    fun parsesOk() {
        val r = EditLineReply.parse("""{"ok":true,"line":"- [ ] Новая строка 📅 2026-07-20"}""")
        assertTrue(r.ok)
        assertEquals("- [ ] Новая строка 📅 2026-07-20", r.line)
        assertNull(r.error)
    }

    @Test
    fun parsesError() {
        val r = EditLineReply.parse("""{"ok":false,"error":"series-date-not-editable"}""")
        assertFalse(r.ok)
        assertNull(r.line)
        assertEquals("series-date-not-editable", r.error)
    }

    @Test
    fun brokenJsonIsFailure() {
        val r = EditLineReply.parse("не json")
        assertFalse(r.ok)
        assertEquals("bad reply json", r.error)
    }

    @Test
    fun humanizesKnownCodes() {
        assertEquals("Дату серии здесь менять нельзя", EditErrorText.humanize("series-date-not-editable"))
        assertEquals("Заголовок не может быть пустым", EditErrorText.humanize("empty-title"))
        assertEquals("Время без даты — сначала укажите дату", EditErrorText.humanize("time-without-date"))
    }

    @Test
    fun humanizesUnknownAndNull() {
        assertEquals("Не удалось сохранить", EditErrorText.humanize(null))
        assertTrue(EditErrorText.humanize("weird-code").startsWith("Не удалось сохранить"))
        assertEquals("Ошибка данных правки", EditErrorText.humanize("bad edits json: SyntaxError"))
    }
}
