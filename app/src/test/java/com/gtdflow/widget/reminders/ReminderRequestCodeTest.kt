package com.gtdflow.widget.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Стабильный requestCode: детерминизм (переживает рестарт процесса), неотрицательность, различимость. */
class ReminderRequestCodeTest {

    @Test
    fun deterministicForSameKey() {
        val key = "Проекты/Работа.md:42:2026-07-21:600"
        assertEquals(ReminderRequestCode.of(key), ReminderRequestCode.of(key))
    }

    @Test
    fun alwaysNonNegative() {
        val keys = listOf(
            "a", "b:1:2026-01-01:0", "файл.md:9999:2026-12-31:1439",
            "Очень/Длинный/Путь с пробелами.md:1:2026-07-21:735",
        )
        for (k in keys) {
            assertTrue("code >= 0 for '$k'", ReminderRequestCode.of(k) >= 0)
        }
    }

    @Test
    fun distinctKeysDifferentCodes() {
        val a = ReminderRequestCode.of("f.md:1:2026-07-21:600")
        val b = ReminderRequestCode.of("f.md:2:2026-07-21:600") // другая строка
        val c = ReminderRequestCode.of("f.md:1:2026-07-22:600") // другая дата
        val d = ReminderRequestCode.of("f.md:1:2026-07-21:660") // другое время
        assertNotEquals(a, b)
        assertNotEquals(a, c)
        assertNotEquals(a, d)
        assertNotEquals(b, c)
    }

    @Test
    fun emptyKeyIsNonNegative() {
        assertTrue(ReminderRequestCode.of("") >= 0)
    }
}
