package com.gtdflow.widget.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Разбор JSON-контракта ядра из фикстуры (src/test/resources/widget_data_fixture.json). */
class WidgetModelsTest {

    private fun fixture(): String =
        requireNotNull(javaClass.getResourceAsStream("/widget_data_fixture.json")) {
            "фикстура widget_data_fixture.json не найдена на тест-classpath"
        }.bufferedReader(Charsets.UTF_8).use { it.readText() }

    private fun data(): WidgetData = WidgetJson.decodeFromString(WidgetData.serializer(), fixture())

    @Test
    fun decodesTodaySection() {
        val data = data()
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

    /** Новые поля элементов (itemKind/rawLine/recurrenceText) — для шторки деталей. */
    @Test
    fun decodesTodayItemDetailFields() {
        val data = data()
        val event = data.today.items[0]
        assertEquals("single-event", event.itemKind)
        assertFalse(event.isSeriesOccurrence)
        assertEquals("- [ ] Встреча 📅 2026-07-20 ⏰ 10:00-11:00 📍 офис", event.rawLine)
        assertEquals(null, event.recurrenceText)

        val task = data.today.items[1]
        assertEquals("task", task.itemKind)
        assertEquals("- [ ] Позвонить врачу ⏳ 2026-07-20 14:00", task.rawLine)
    }

    /** Секция агенды: дни от сегодня, серия помечена series-occurrence + recurrenceText. */
    @Test
    fun decodesAgendaSection() {
        val data = data()
        assertEquals(2, data.agenda.days.size)
        val day0 = data.agenda.days[0]
        assertEquals("2026-07-20", day0.date)
        assertEquals(1, day0.items.size)
        val series = day0.items[0]
        assertTrue(series.isSeriesOccurrence)
        assertEquals("every weekday at 09:00-09:30", series.recurrenceText)
        // пустые дни остаются в контракте (виджет их отфильтрует)
        assertTrue(data.agenda.days[1].items.isEmpty())
    }

    @Test
    fun decodesInboxAndNamespaces() {
        val data = data()
        assertEquals("Все", data.inbox.namespace)
        assertEquals(1, data.inbox.items.size)
        val item = data.inbox.items[0]
        assertEquals("Купить билеты", item.title)
        assertEquals("Работа", item.namespace) // метка пространства для агрегата «Все»
        assertEquals("вокзал", item.location)
        assertEquals(listOf("Работа", "Жизнь"), data.namespaces.map { it.name })
        assertTrue(data.errors.isEmpty())
    }

    /** Старый контракт без agenda/itemKind/rawLine (обратная совместимость через дефолты). */
    @Test
    fun decodesLegacyContractWithoutNewFields() {
        val json = """{"today":{"date":"2026-01-01","items":[
            |{"kind":"task","title":"X","file":"a.md","line":2,"namespace":"Общее"}],
            |"generatedAt":"2026-01-01T00:00"},
            |"inbox":{"namespace":"Общее","items":[
            |{"title":"Y","file":"b.md","line":5}]}}""".trimMargin()
        val data = WidgetJson.decodeFromString(WidgetData.serializer(), json)
        assertTrue(data.agenda.days.isEmpty())
        val item = data.today.items[0]
        assertEquals("", item.itemKind)
        assertEquals("", item.rawLine)
        assertEquals(null, item.recurrenceText)
        // inbox-item без namespace → дефолт "" (агрегат просто не покажет метку)
        assertEquals("", data.inbox.items[0].namespace)
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
