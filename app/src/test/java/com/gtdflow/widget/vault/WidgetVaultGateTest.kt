package com.gtdflow.widget.vault

import org.junit.Assert.assertEquals
import org.junit.Test

/** Стартовое состояние виджета по доступу к vault (дефект 4: потеря доступа ≠ «Загрузка…»). */
class WidgetVaultGateTest {

    @Test
    fun notConfiguredSelectsVault() {
        assertEquals(WidgetVaultGate.SELECT_VAULT, WidgetVaultGate.of(configured = false, hasAccess = false))
        // даже если доступ формально есть, но vault не выбран — выбираем vault
        assertEquals(WidgetVaultGate.SELECT_VAULT, WidgetVaultGate.of(configured = false, hasAccess = true))
    }

    @Test
    fun configuredButNoAccessIsAccessLost() {
        assertEquals(WidgetVaultGate.ACCESS_LOST, WidgetVaultGate.of(configured = true, hasAccess = false))
    }

    @Test
    fun configuredWithAccessIsReady() {
        assertEquals(WidgetVaultGate.READY, WidgetVaultGate.of(configured = true, hasAccess = true))
    }
}
