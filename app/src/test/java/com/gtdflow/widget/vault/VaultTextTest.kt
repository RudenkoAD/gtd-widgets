package com.gtdflow.widget.vault

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Текстовые преобразования vault: frontmatter входящих и отметка чекбокса (чистый Kotlin). */
class VaultTextTest {

    // --- InboxFileText ---

    @Test
    fun newFileHasInboxFlag() {
        val out = InboxFileText.newFile("- [ ] задача")
        assertEquals("---\ngtd-inbox: true\n---\n\n- [ ] задача\n", out)
    }

    @Test
    fun ensureFrontmatterAddsWhenMissing() {
        val out = InboxFileText.ensureInboxFrontmatter("текст без frontmatter")
        assertTrue(out.startsWith("---\ngtd-inbox: true\n---"))
        assertTrue(out.contains("текст без frontmatter"))
    }

    @Test
    fun ensureFrontmatterIdempotentWhenAlreadyTrue() {
        val src = "---\ngtd-inbox: true\ntitle: X\n---\nтело"
        assertEquals(src, InboxFileText.ensureInboxFrontmatter(src))
    }

    @Test
    fun ensureFrontmatterInsertsKeyIntoExistingBlock() {
        val src = "---\ntitle: X\n---\nтело"
        val out = InboxFileText.ensureInboxFrontmatter(src)
        assertTrue(out.contains("gtd-inbox: true"))
        assertTrue(out.contains("title: X"))
        assertTrue(out.trimEnd().endsWith("тело"))
    }

    @Test
    fun appendCaptureEndsWithSingleNewline() {
        val out = InboxFileText.appendCapture("---\ngtd-inbox: true\n---\n\nстарое", "- [ ] новое")
        assertTrue(out.endsWith("- [ ] новое\n"))
        assertTrue(out.contains("старое"))
    }

    // --- TaskLineToggle ---

    @Test
    fun marksDoneByLineNumber() {
        val content = "заголовок\n- [ ] Позвонить врачу 📅 2026-07-20\nхвост"
        val out = TaskLineToggle.markDone(content, 2, "Позвонить врачу")
        assertEquals(
            "заголовок\n- [x] Позвонить врачу 📅 2026-07-20\nхвост",
            out,
        )
    }

    @Test
    fun marksDoneByTextWhenLineShifted() {
        val content = "новая строка\nзаголовок\n- [ ] Позвонить врачу\nхвост"
        // номер (2) указывает не на чекбокс — ищем по тексту
        val out = TaskLineToggle.markDone(content, 2, "Позвонить врачу")
        assertTrue(out!!.contains("- [x] Позвонить врачу"))
    }

    @Test
    fun returnsNullWhenNoMatch() {
        val content = "- [ ] другое дело"
        assertNull(TaskLineToggle.markDone(content, 1, "Не найдётся"))
    }

    // --- TaskLineToggle: сравнение по словам (title ядра со схлопнутыми пробелами и
    //     вырезанными инлайновыми полями не обязан быть подстрокой сырой строки) ---

    /** Дефект 2: инлайновое поле (⏫) между слов не должно уводить отметку на чужую строку. */
    @Test
    fun marksTappedLineNotAnotherWhenInlineFieldSplitsTitle() {
        // строка 1 — та, по которой тапнули (title ядра = "buy milk soon", ⏫ вырезан);
        // строка 3 — похожая задача, которую нельзя трогать.
        val content = "- [ ] buy milk ⏫ soon\nзаметка\n- [ ] buy milk soon"
        val out = TaskLineToggle.markDone(content, 1, "buy milk soon")
        assertEquals("- [x] buy milk ⏫ soon\nзаметка\n- [ ] buy milk soon", out)
    }

    /** Дефект 2: двойной пробел в строке (title схлопнут в один) — отметка по номеру всё равно проходит. */
    @Test
    fun marksDoneDespiteCollapsedInnerWhitespace() {
        val content = "- [ ] Позвонить  врачу"
        val out = TaskLineToggle.markDone(content, 1, "Позвонить врачу")
        assertEquals("- [x] Позвонить  врачу", out)
    }

    /** Дефект 2: неоднозначный fallback (номер мимо, два одинаковых кандидата) — не угадываем. */
    @Test
    fun fallbackDoesNotGuessAmbiguousDuplicate() {
        // строка 1 (номер) — не чекбокс; ниже два идентичных незакрытых кандидата.
        val content = "заголовок\n- [ ] купить хлеб\n- [ ] купить хлеб"
        assertNull(TaskLineToggle.markDone(content, 1, "купить хлеб"))
    }

    /** Поля с датой (📅 + дата как отдельные «слова») не мешают подпоследовательности. */
    @Test
    fun marksDoneWithTrailingDateField() {
        val content = "- [ ] Позвонить врачу 📅 2026-07-20"
        val out = TaskLineToggle.markDone(content, 1, "Позвонить врачу")
        assertEquals("- [x] Позвонить врачу 📅 2026-07-20", out)
    }

    // --- VaultWriter.captureAppend: не перезаписывать существующий файл при сбое чтения ---

    /** Дефект 1: чтение не удалось (null) → null (вызывающий прервётся, файл не тронут). */
    @Test
    fun captureAppendAbortsOnReadFailure() {
        assertNull(VaultWriter.captureAppend(null, "- [ ] новое"))
    }

    /** Дефект 1: реально пустой файл ("") → дописать (не спутать с провалом чтения). */
    @Test
    fun captureAppendWritesIntoEmptyFile() {
        val out = VaultWriter.captureAppend("", "- [ ] новое")
        assertTrue(out!!.endsWith("- [ ] новое\n"))
    }

    /** Дефект 1: существующие входящие сохраняются, новая строка дописывается в конец. */
    @Test
    fun captureAppendPreservesExistingTasks() {
        val existing = "---\ngtd-inbox: true\n---\n\n- [ ] старое дело"
        val out = VaultWriter.captureAppend(existing, "- [ ] новое")!!
        assertTrue(out.contains("- [ ] старое дело"))
        assertTrue(out.endsWith("- [ ] новое\n"))
    }
}
