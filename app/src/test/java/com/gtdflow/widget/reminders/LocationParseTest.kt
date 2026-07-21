package com.gtdflow.widget.reminders

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Разбор строки места «lat, lng»: координаты парсятся, имена/мусор/выход за диапазон → null. */
class LocationParseTest {

    @Test
    fun parsesLatLngWithSpace() {
        val c = LocationParse.parse("55.7558, 37.6173")!!
        assertEquals(55.7558, c.lat, 1e-9)
        assertEquals(37.6173, c.lng, 1e-9)
    }

    @Test
    fun parsesWithoutSpace() {
        val c = LocationParse.parse("55.7558,37.6173")!!
        assertEquals(55.7558, c.lat, 1e-9)
        assertEquals(37.6173, c.lng, 1e-9)
    }

    @Test
    fun trimsSurroundingWhitespace() {
        val c = LocationParse.parse("  55.75 , 37.61 ")!!
        assertEquals(55.75, c.lat, 1e-9)
        assertEquals(37.61, c.lng, 1e-9)
    }

    @Test
    fun parsesNegativeCoordinates() {
        val c = LocationParse.parse("-33.8688, 151.2093")!!
        assertEquals(-33.8688, c.lat, 1e-9)
        assertEquals(151.2093, c.lng, 1e-9)
    }

    @Test
    fun placeNameIsNull() {
        assertNull(LocationParse.parse("Кафе на Тверской"))
    }

    @Test
    fun singlePartIsNull() {
        assertNull(LocationParse.parse("55.75"))
    }

    @Test
    fun threePartsIsNull() {
        assertNull(LocationParse.parse("55.75, 37.61, 0"))
    }

    @Test
    fun nonNumericIsNull() {
        assertNull(LocationParse.parse("abc, def"))
    }

    @Test
    fun latitudeOutOfRangeIsNull() {
        assertNull(LocationParse.parse("200.0, 37.0"))
    }

    @Test
    fun longitudeOutOfRangeIsNull() {
        assertNull(LocationParse.parse("55.0, 200.0"))
    }

    @Test
    fun blankAndNullAreNull() {
        assertNull(LocationParse.parse(""))
        assertNull(LocationParse.parse("   "))
        assertNull(LocationParse.parse(null))
    }
}
