package com.gtdflow.widget.inbox

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Выбор пространства-цели захвата: «Все»/«Общее» → «Общее» (null), имя — как есть. */
class CaptureNamespaceTest {

    @Test
    fun aggregateAllFallsBackToCommon() {
        // захват при «Все» пишет в «Общее» (namespace=null в captureTargetPath)
        assertNull(CaptureNamespace.forWrite(InboxWidgetState.ALL_NAMESPACE))
    }

    @Test
    fun commonMapsToNull() {
        assertNull(CaptureNamespace.forWrite(InboxWidgetState.DEFAULT_NAMESPACE))
    }

    @Test
    fun namedNamespacePassedThrough() {
        assertEquals("Работа", CaptureNamespace.forWrite("Работа"))
        assertEquals("Жизнь", CaptureNamespace.forWrite("Жизнь"))
    }
}
