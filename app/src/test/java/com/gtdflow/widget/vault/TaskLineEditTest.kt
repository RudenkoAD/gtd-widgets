package com.gtdflow.widget.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Замена одной строки файла: точный якорь (шторка) и поиск чекбокса (входящие). */
class TaskLineEditTest {

    // --- anchorExact: точный якорь по номеру + сырой строке (лента «Сегодня»/«Агенды») ---

    @Test
    fun anchorExactMatchesByLineAndRawLine() {
        val content = "заголовок\n- [ ] Встреча 📅 2026-07-20\nхвост"
        val idx = TaskLineEdit.anchorExact(content, 2, "- [ ] Встреча 📅 2026-07-20")
        assertEquals(1, idx)
    }

    @Test
    fun anchorExactNullWhenRawLineDiffers() {
        // строка на этом номере другая (файл изменился синком) → писать нельзя
        val content = "заголовок\n- [ ] Другое событие\nхвост"
        assertNull(TaskLineEdit.anchorExact(content, 2, "- [ ] Встреча 📅 2026-07-20"))
    }

    @Test
    fun anchorExactNullWhenLineOutOfRange() {
        assertNull(TaskLineEdit.anchorExact("одна строка", 5, "одна строка"))
    }

    @Test
    fun anchorExactToleratesTrailingCr() {
        val content = "a\r\n- [ ] Задача\r\nb"
        val idx = TaskLineEdit.anchorExact(content, 2, "- [ ] Задача")
        assertEquals(1, idx)
    }

    // --- locate: поиск незакрытого чекбокса по номеру+тексту (входящие, без rawLine ядра) ---

    @Test
    fun locateFindsUncheckedByNumberAndTitle() {
        val content = "# файл\n- [ ] Купить хлеб 📍 магазин\n- [ ] другое"
        val located = TaskLineEdit.locate(content, 2, "Купить хлеб")
        assertEquals(1, located!!.index)
        assertEquals("- [ ] Купить хлеб 📍 магазин", located.rawLine)
    }

    @Test
    fun locateNullWhenNoMatch() {
        assertNull(TaskLineEdit.locate("- [ ] совсем другое", 1, "Не найдётся"))
    }

    // --- replaceAt: замена по индексу с сохранением хвостового '\r' ---

    @Test
    fun replaceAtSwapsLinePreservingRest() {
        val content = "верх\n- [ ] старое\nниз"
        val out = TaskLineEdit.replaceAt(content, 1, "- [ ] новое 📅 2026-07-21")
        assertEquals("верх\n- [ ] новое 📅 2026-07-21\nниз", out)
    }

    @Test
    fun replaceAtKeepsCrlf() {
        val content = "верх\r\n- [ ] старое\r\nниз"
        val out = TaskLineEdit.replaceAt(content, 1, "- [ ] новое")
        assertEquals("верх\r\n- [ ] новое\r\nниз", out)
    }

    /** Полный цикл входящих: locate → replaceAt по найденному индексу. */
    @Test
    fun locateThenReplaceRoundTrip() {
        val content = "- [ ] Позвонить врачу"
        val located = TaskLineEdit.locate(content, 1, "Позвонить врачу")!!
        val out = TaskLineEdit.replaceAt(content, located.index, "- [ ] Позвонить врачу 📍 клиника")
        assertEquals("- [ ] Позвонить врачу 📍 клиника", out)
    }
}
