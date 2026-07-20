package com.gtdflow.widget.inbox

import com.gtdflow.widget.engine.InboxItem
import com.gtdflow.widget.engine.InboxSection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Оптимистичные патчи секции «Входящие» (чистый Kotlin, без Android). */
class InboxOptimisticTest {

    private fun item(title: String, file: String, line: Int, location: String? = null) =
        InboxItem(title = title, file = file, line = line, location = location)

    private fun section(vararg items: InboxItem) =
        InboxSection(namespace = "Все", items = items.toList())

    @Test
    fun removeLineDropsMatchingFileAndLine() {
        val s = section(
            item("A", "GTD/Inbox.md", 5),
            item("B", "GTD/Inbox.md", 7),
            item("C", "Work/Inbox.md", 5),
        )
        val out = InboxOptimistic.removeLine(s, "GTD/Inbox.md", 5)
        assertEquals(listOf("B", "C"), out.items.map { it.title })
    }

    @Test
    fun removeLineKeepsSameFileDifferentLine() {
        // одинаковый файл, но другой номер — НЕ трогаем
        val s = section(item("A", "GTD/Inbox.md", 5), item("B", "GTD/Inbox.md", 6))
        val out = InboxOptimistic.removeLine(s, "GTD/Inbox.md", 6)
        assertEquals(listOf("A"), out.items.map { it.title })
    }

    @Test
    fun removeLineNoMatchLeavesListIntact() {
        val s = section(item("A", "GTD/Inbox.md", 5))
        val out = InboxOptimistic.removeLine(s, "GTD/Inbox.md", 99)
        assertEquals(listOf("A"), out.items.map { it.title })
    }

    @Test
    fun editLineUpdatesTitleAndLocationInPlaceOnly() {
        val s = section(
            item("Старый", "GTD/Inbox.md", 5, location = "Дом"),
            item("Другой", "GTD/Inbox.md", 8),
        )
        val out = InboxOptimistic.editLine(s, "GTD/Inbox.md", 5, "Новый", "Офис")
        assertEquals(listOf("Новый", "Другой"), out.items.map { it.title })
        assertEquals("Офис", out.items[0].location)
        assertEquals(5, out.items[0].line) // остальные поля не тронуты
    }

    @Test
    fun editLineBlankLocationBecomesNull() {
        val s = section(item("T", "f.md", 1, location = "Старое место"))
        val out = InboxOptimistic.editLine(s, "f.md", 1, "T2", "   ")
        assertEquals(null, out.items[0].location)
    }

    @Test
    fun prependAddsToFront() {
        val s = section(item("A", "f.md", 1))
        val out = InboxOptimistic.prepend(s, item("NEW", "f.md", 0))
        assertEquals(listOf("NEW", "A"), out.items.map { it.title })
        assertEquals("Все", out.namespace) // метка секции сохранена
    }

    @Test
    fun editLineNoMatchReturnsEquivalentSection() {
        val s = section(item("A", "f.md", 1))
        val out = InboxOptimistic.editLine(s, "other.md", 1, "X", null)
        assertEquals(s.items.map { it.title }, out.items.map { it.title })
        assertSame(s.items[0], out.items[0]) // не пересоздаём несовпавшие элементы
    }
}
