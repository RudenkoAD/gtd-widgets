package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Чистое форматирование времени ленты «Сегодня» (JVM, без Android). */
class TimeUtilTest {

    @Test
    fun minutesToHhmmPads() {
        assertEquals("00:00", TimeUtil.minutesToHhmm(0))
        assertEquals("10:00", TimeUtil.minutesToHhmm(600))
        assertEquals("14:05", TimeUtil.minutesToHhmm(845))
        assertEquals("23:59", TimeUtil.minutesToHhmm(24 * 60 - 1))
    }

    @Test
    fun minutesToHhmmClamps() {
        assertEquals("00:00", TimeUtil.minutesToHhmm(-30))
        assertEquals("23:59", TimeUtil.minutesToHhmm(9999))
    }

    @Test
    fun formatRange() {
        assertEquals("", TimeUtil.formatRange(null, null))
        assertEquals("14:00", TimeUtil.formatRange(840, null))
        // тире — U+2013
        assertEquals("10:00–11:00", TimeUtil.formatRange(600, 660))
    }

    @Test
    fun hourOf() {
        assertEquals(0, TimeUtil.hourOf(0))
        assertEquals(14, TimeUtil.hourOf(840))
        assertEquals(23, TimeUtil.hourOf(24 * 60 - 1))
    }
}
