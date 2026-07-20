package com.gtdflow.widget.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Кэш содержимого vault по docId+mtime: попадания, инвалидация по mtime, сброс vault. */
class VaultContentCacheTest {

    private fun fresh() = VaultContentCache().apply { bind("tree://v1") }

    @Test
    fun hitWhenSameMtime() {
        val c = fresh()
        c.put("doc1", 1000L, "content-A")
        assertEquals("content-A", c.get("doc1", 1000L))
    }

    @Test
    fun missWhenMtimeGrew() {
        // файл изменился (mtime вырос) — кэш не отдаёт старое содержимое
        val c = fresh()
        c.put("doc1", 1000L, "content-A")
        assertNull(c.get("doc1", 2000L))
    }

    @Test
    fun missWhenUnknownDoc() {
        val c = fresh()
        assertNull(c.get("ghost", 1000L))
    }

    @Test
    fun mtimeNonPositiveNeverCachedNorServed() {
        val c = fresh()
        c.put("doc1", 0L, "x") // недоверенный mtime — не кэшируем
        assertEquals(0, c.size())
        c.put("doc1", 1000L, "real")
        assertNull(c.get("doc1", 0L)) // запрос с mtime<=0 — всегда промах
        assertNull(c.get("doc1", -5L))
    }

    @Test
    fun updateReplacesContentForNewMtime() {
        val c = fresh()
        c.put("doc1", 1000L, "old")
        c.update("doc1", 1500L, "new") // наша запись — обновили локально
        assertEquals("new", c.get("doc1", 1500L))
        assertNull(c.get("doc1", 1000L))
    }

    @Test
    fun updateWithUnknownMtimeInvalidates() {
        val c = fresh()
        c.put("doc1", 1000L, "old")
        c.update("doc1", 0L, "whatever") // mtime неизвестен → забыть, пусть перечитается
        assertEquals(0, c.size())
        assertNull(c.get("doc1", 1000L))
    }

    @Test
    fun invalidateDropsEntry() {
        val c = fresh()
        c.put("doc1", 1000L, "a")
        c.invalidate("doc1")
        assertNull(c.get("doc1", 1000L))
    }

    @Test
    fun rebindToNewVaultClearsCache() {
        val c = fresh()
        c.put("doc1", 1000L, "a")
        c.bind("tree://v2") // сменили vault — docId прежнего дерева невалидны
        assertEquals(0, c.size())
        assertNull(c.get("doc1", 1000L))
    }

    @Test
    fun rebindSameVaultKeepsCache() {
        val c = fresh()
        c.put("doc1", 1000L, "a")
        c.bind("tree://v1") // тот же URI — кэш сохраняется
        assertEquals("a", c.get("doc1", 1000L))
    }
}
