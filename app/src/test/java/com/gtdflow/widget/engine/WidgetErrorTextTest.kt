package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Приведение сбоя движка/чтения к тексту для виджета (дефект: креш/вечная «Загрузка…»). */
class WidgetErrorTextTest {

    @Test
    fun usesExceptionMessage() {
        val msg = WidgetErrorText.forThrowable(IllegalStateException("boom"))
        assertEquals("boom", msg)
    }

    @Test
    fun fallsBackToClassNameWhenNoMessage() {
        assertEquals("NullPointerException", WidgetErrorText.forThrowable(NullPointerException()))
    }

    @Test
    fun collapsesNewlinesForOneLineWidget() {
        val msg = WidgetErrorText.forThrowable(RuntimeException("line1\nline2"))
        assertTrue(!msg.contains('\n'))
        assertTrue(msg.contains("line1") && msg.contains("line2"))
    }

    @Test
    fun clipsVeryLongMessages() {
        val msg = WidgetErrorText.forThrowable(RuntimeException("x".repeat(500)))
        assertTrue(msg.length <= 160)
        assertTrue(msg.endsWith("…"))
    }

    @Test
    fun widgetLineHasPrefix() {
        // Регрессия дефекта: движок без нативной библиотеки бросал такое сообщение —
        // виджет обязан показать его текстом, а не висеть в «Загрузка…».
        val t = RuntimeException(
            "The so library must be initialized before createContext! QuickJSLoader.init should be called",
        )
        val line = WidgetErrorText.widgetLine(t)
        assertTrue(line.startsWith("Ошибка: "))
        assertTrue(line.contains("QuickJSLoader.init"))
    }
}
