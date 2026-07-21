package com.gtdflow.widget.reminders

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Контракт тумблера «Напоминать по месту» vs системные разрешения (README: «без
 * разрешений тумблер держится выключенным»). Регрессия: раньше отказ в геолокации
 * оставлял оптимистично включённый тумблер ВКЛ навсегда.
 */
class ReminderPolicyTest {

    @Test
    fun foregroundDeniedRollsPlaceOff() {
        // отказ в базовой геолокации → тумблер обязан вернуться в OFF
        assertFalse(ReminderPolicy.placeEnabledAfterForegroundResult(granted = false))
    }

    @Test
    fun foregroundGrantedKeepsPlaceOn() {
        assertTrue(ReminderPolicy.placeEnabledAfterForegroundResult(granted = true))
    }

    @Test
    fun backgroundDeniedKeepsPlaceOnForStagedRetry() {
        // фон — поэтапное уточнение поверх уже включённого места: его отказ тумблер
        // НЕ выключает, иначе кнопка «Разрешить фон» исчезла бы и фон стал недостижим
        assertTrue(ReminderPolicy.placeEnabledAfterBackgroundResult(currentlyEnabled = true))
    }

    @Test
    fun backgroundResultDoesNotFlipDisabledOn() {
        assertFalse(ReminderPolicy.placeEnabledAfterBackgroundResult(currentlyEnabled = false))
    }
}
