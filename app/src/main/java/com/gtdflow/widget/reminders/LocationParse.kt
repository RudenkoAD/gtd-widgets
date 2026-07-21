package com.gtdflow.widget.reminders

/**
 * Разбор строки места «lat, lng» в координаты (ЧИСТО, тестируется на JVM).
 *
 * Строгий формат: РОВНО две части через запятую, обе — числа в допустимом диапазоне
 * (широта −90..90, долгота −180..180). Разделитель дробной части — точка (так пишут
 * координаты в vault). Если строка не парсится как координаты (это имя места вроде
 * «Кафе на Тверской») — возвращаем null, и вызывающий код падает на Geocoder по имени.
 */
object LocationParse {

    /** Координата: широта/долгота в градусах. */
    data class LatLng(val lat: Double, val lng: Double)

    fun parse(location: String?): LatLng? {
        val raw = location?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val parts = raw.split(',')
        if (parts.size != 2) return null
        val lat = parts[0].trim().toDoubleOrNull() ?: return null
        val lng = parts[1].trim().toDoubleOrNull() ?: return null
        if (lat.isNaN() || lng.isNaN()) return null
        if (lat < -90.0 || lat > 90.0) return null
        if (lng < -180.0 || lng > 180.0) return null
        return LatLng(lat, lng)
    }
}
