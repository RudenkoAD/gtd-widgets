package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Разбор JSON-контракта ядра из фикстуры (src/test/resources/widget_data_fixture.json). */
class WidgetModelsTest {

    private fun fixture(): String =
        requireNotNull(javaClass.getResourceAsStream("/widget_data_fixture.json")) {
            "фикстура widget_data_fixture.json не найдена на тест-classpath"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test
    fun decodesTodaySection() {
        val data = WidgetJson.decodeFromString(WidgetData.serializer(), fixture())
        assertEquals("2026-07-20", data.today.date)
        assertEquals(2, data.today.items.size)

        val event = data.today.items[0]
        assertTrue(event.isEvent)
        assertEquals("Встреча", event.title)
        assertEquals(600, event.startMinutes)
        assertEquals(660, event.endMinutes)
        assertEquals("офис", event.location)

        val task = data.today.items[1]
        assertEquals("task", task.kind)
        assertEquals(840, task.startMinutes)
        assertEquals(null, task.endMinutes)
        assertEquals(null, task.location)
    }

    @Test
    fun decodesInboxAndNamespaces() {
        val data = WidgetJson.decodeFromString(WidgetData.serializer(), fixture())
        assertEquals("Работа", data.inbox.namespace)
        assertTrue(data.inbox.items.isEmpty())
        assertEquals(listOf("Работа", "Жизнь"), data.namespaces.map { it.name })
        assertTrue(data.errors.isEmpty())
    }

    @Test
    fun ignoresUnknownKeys() {
        val json = """{"today":{"date":"x","items":[],"generatedAt":"x","extra":1},
            |"inbox":{"namespace":"n"},"surprise":true}""".trimMargin()
        val data = WidgetJson.decodeFromString(WidgetData.serializer(), json)
        assertEquals("x", data.today.date)
        assertEquals("n", data.inbox.namespace)
    }
}
